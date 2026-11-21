package ic2.core.block.machine.tileentity;

import ic2.api.network.INetworkClientTileEntityEventListener;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ContainerBase;
import ic2.core.IC2;
import ic2.core.IHasGui;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.container.ContainerSortingMachine;
import ic2.core.network.GrowingBuffer;
import ic2.core.profile.NotClassic;
import ic2.core.ref.Ic2BlockEntities;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

@NotClassic
public class TileEntitySortingMachine extends TileEntityElectricMachine
    implements IHasGui, INetworkClientTileEntityEventListener, IUpgradableBlock {
  public static final int defaultTier = 2;
  public final InvSlotUpgrade upgradeSlot;
  public final InvSlot buffer;
  private final ItemStack[][] filters;
  // Retain the legacy field/network/NBT name; D now designates the selected INPUT face.
  public Direction defaultRoute = Direction.DOWN;
  private final ItemStack[] batchItems = new ItemStack[6];
  private final ItemStack[] batchRules = new ItemStack[6];
  private final int[] batchRemaining = new int[6];

  public TileEntitySortingMachine(BlockPos pos, BlockState state) {
    super(Ic2BlockEntities.SORTING_MACHINE, pos, state, 15000, 2, false);
    this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
    this.buffer = new InvSlot(this, "Buffer", InvSlot.Access.IO, 11);
    this.filters = new ItemStack[6][7];
  }

  @Override
  protected void loadAdditional(
      CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    super.loadAdditional(nbt, registries);
    for (ItemStack[] sideFilters : this.filters) {
      java.util.Arrays.fill(sideFilters, ItemStack.EMPTY);
    }
    ListTag filtersTag = ic2.compat.NbtCompat.getList(nbt, "filters", 10);

    for (int i = 0; i < filtersTag.size(); i++) {
      CompoundTag filterTag = filtersTag.getCompoundOrEmpty(i);
      int index = filterTag.getByteOr("index", (byte) 0) & 255;
      if (index >= 42) {
        continue;
      }
      ItemStack stack = ic2.compat.NbtCompat.readItem(registries, filterTag);
      this.filters[index / 7][index % 7] = stack;
    }

    int defaultRouteIdx = nbt.getByteOr("defaultroute", (byte) 0);
    if (defaultRouteIdx >= 0 && defaultRouteIdx < Util.ALL_DIRS.length) {
      this.defaultRoute = Util.ALL_DIRS[defaultRouteIdx];
    }
    java.util.Arrays.fill(this.batchRemaining, 0);
    java.util.Arrays.fill(this.batchItems, ItemStack.EMPTY);
    java.util.Arrays.fill(this.batchRules, ItemStack.EMPTY);
    for (var element : ic2.compat.NbtCompat.getList(nbt, "routingBatches", 10)) {
      if (!(element instanceof CompoundTag tag)) continue;
      int side = tag.getIntOr("side", -1);
      if (side < 0 || side >= 6 || Util.ALL_DIRS[side] == this.defaultRoute) continue;
      ItemStack item = ic2.compat.NbtCompat.readItem(registries, tag.getCompoundOrEmpty("item"));
      ItemStack rule = ic2.compat.NbtCompat.readItem(registries, tag.getCompoundOrEmpty("rule"));
      int remaining = tag.getIntOr("remaining", 0);
      if (remaining > 0 && remaining <= rule.getCount() && matchesFilter(rule, item)) {
        this.batchItems[side] = item.copyWithCount(1);
        this.batchRules[side] = rule;
        this.batchRemaining[side] = remaining;
      }
    }
  }

  @Override
  public void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    super.saveAdditional(nbt, registries);
    ListTag filtersTag = new ListTag();

    for (int i = 0; i < 42; i++) {
      ItemStack stack = this.filters[i / 7][i % 7];
      if (!StackUtil.isEmpty(stack)) {
        CompoundTag contentTag = new CompoundTag();
        contentTag.putByte("index", (byte) i);
        // save() returns the merged tag instead of mutating the prefix
        filtersTag.add(ic2.compat.NbtCompat.saveItem(stack, registries, contentTag));
      }
    }

    nbt.put("filters", filtersTag);
    nbt.putByte("defaultroute", (byte) this.defaultRoute.ordinal());
    ListTag batches = new ListTag();
    for (int side = 0; side < 6; side++) {
      if (this.batchRemaining[side] <= 0) continue;
      CompoundTag tag = new CompoundTag();
      tag.putInt("side", side);
      tag.putInt("remaining", this.batchRemaining[side]);
      tag.put(
          "item",
          ic2.compat.NbtCompat.saveItem(this.batchItems[side], registries, new CompoundTag()));
      tag.put(
          "rule",
          ic2.compat.NbtCompat.saveItem(this.batchRules[side], registries, new CompoundTag()));
      batches.add(tag);
    }
    nbt.put("routingBatches", batches);
  }

  @Override
  protected void onLoaded() {
    super.onLoaded();
    if (IC2.sideProxy.isSimulating()) {
      this.setUpgradableBlock();
    }
  }

  @Override
  protected void updateEntityServer() {
    super.updateEntityServer();
    if (this.energy.getEnergy() < 20.0) return;
    boolean changed = this.pullFromInput();
    for (Direction side : Util.ALL_DIRS) {
      if (side == this.defaultRoute || this.energy.getEnergy() < 20.0) continue;
      var target = this.adjacentHandler(side);
      if (target == null) continue;
      int index = side.ordinal();
      // Changing a ghost rule cancels the pending batch before another item can leave.
      if (this.batchRemaining[index] > 0
          && (!this.hasBatchRule(side) || this.countBuffered(this.batchItems[index]) == 0)) {
        this.batchRemaining[index] = 0;
        changed = true;
      }
      if (this.batchRemaining[index] == 0) {
        if (this.tryBeginBatch(side, target)) changed = true;
        continue;
      }
      if (this.transferOne(side, target, this.batchItems[index])) {
        this.batchRemaining[index]--;
        changed = true;
      }
      // One ordinary ejector upgrade transfers 4^(1-1) = 1 item/face/server tick.
    }
    if (changed) this.setChanged();
  }

  private boolean pullFromInput() {
    var source = this.adjacentHandler(this.defaultRoute);
    if (source == null) return false;
    var destination = new WorldlyContainerWrapper(this, this.defaultRoute);
    for (int slot = 0; slot < source.size(); slot++) {
      var resource = source.getResource(slot);
      if (resource.isEmpty() || !this.matchesWhitelist(this.defaultRoute, resource.toStack()))
        continue;
      try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
        if (source.extract(slot, resource, 1, transaction) != 1
            || destination.insert(resource, 1, transaction) != 1) continue;
        transaction.commit();
      }
      return true;
    }
    return false;
  }

  private ResourceHandler<ItemResource> adjacentHandler(Direction side) {
    var pos = this.worldPosition.relative(side);
    var handler = this.level.getCapability(Capabilities.Item.BLOCK, pos, side.getOpposite());
    if (handler != null) return handler;
    var target = this.level.getBlockEntity(pos);
    if (target instanceof net.minecraft.world.WorldlyContainer container) {
      return new WorldlyContainerWrapper(container, side.getOpposite());
    }
    if (target instanceof net.minecraft.world.Container container) {
      return VanillaContainerWrapper.of(container);
    }
    return null;
  }

  public boolean hasAdjacentInventory(Direction side) {
    return this.level != null && this.adjacentHandler(side) != null;
  }

  private boolean matchesWhitelist(Direction side, ItemStack stack) {
    for (ItemStack filter : this.getFilterSlots(side)) {
      if (matchesFilter(filter, stack)) return true;
    }
    return false;
  }

  private int countBuffered(ItemStack item) {
    int available = 0;
    for (ItemStack candidate : this.buffer) {
      if (!StackUtil.isEmpty(candidate) && ItemStack.isSameItemSameComponents(candidate, item)) {
        available += candidate.getCount();
      }
    }
    return available;
  }

  private boolean transferOne(
      Direction side, ResourceHandler<ItemResource> target, ItemStack item) {
    var resource = ItemResource.of(item);
    var source = new WorldlyContainerWrapper(this, side);
    try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
      if (source.extract(resource, 1, transaction) != 1
          || target.insert(resource, 1, transaction) != 1) return false;
      transaction.commit();
    }
    this.energy.useEnergy(20.0);
    return true;
  }

  private boolean tryBeginBatch(Direction side, ResourceHandler<ItemResource> target) {
    int index = side.ordinal();
    for (ItemStack rule : this.getFilterSlots(side)) {
      if (StackUtil.isEmpty(rule)) continue;
      for (ItemStack item : this.buffer) {
        if (!matchesFilter(rule, item)
            || this.countBuffered(item) < rule.getCount()
            || !this.energy.canUseEnergy(rule.getCount() * 20.0)) continue;
        // Save an independent snapshot: committing extraction can mutate the buffer's stack.
        ItemStack batchItem = item.copyWithCount(1);
        if (!this.transferOne(side, target, batchItem)) continue;
        this.batchItems[index] = batchItem;
        this.batchRules[index] = rule.copy();
        this.batchRemaining[index] = rule.getCount() - 1;
        return true;
      }
    }
    return false;
  }

  private boolean hasBatchRule(Direction side) {
    int index = side.ordinal();
    for (ItemStack rule : this.getFilterSlots(side)) {
      if (!StackUtil.isEmpty(rule)
          && rule.getCount() == this.batchRules[index].getCount()
          && ItemStack.isSameItemSameComponents(rule, this.batchRules[index])) return true;
    }
    return false;
  }

  @Override
  public int[] getSlotsForFace(Direction side) {
    return java.util.stream.IntStream.range(
            this.getBaseIndex(this.buffer), this.getBaseIndex(this.buffer) + this.buffer.size())
        .toArray();
  }

  @Override
  public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side) {
    return side == this.defaultRoute
        && this.matchesWhitelist(side, stack)
        && super.canPlaceItemThroughFace(index, stack, side);
  }

  @Override
  public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
    return side != this.defaultRoute
        && this.matchesWhitelist(side, stack)
        && super.canTakeItemThroughFace(index, stack, side);
  }

  private static boolean matchesFilter(ItemStack filter, ItemStack stack) {
    if (StackUtil.isEmpty(filter) || !StackUtil.checkItemEquality(filter, stack)) {
      return false;
    }
    // Keep IC2's existing exceptions for charge/energy in CUSTOM_DATA, but compare the
    // independent modern components as well: names, potion contents, enchantments, etc.
    var customData = net.minecraft.core.component.DataComponents.CUSTOM_DATA;
    if (!filter.has(customData) && !stack.has(customData)) {
      return ItemStack.isSameItemSameComponents(filter, stack);
    }
    ItemStack filterComponents = filter.copy();
    ItemStack stackComponents = stack.copy();
    filterComponents.remove(customData);
    stackComponents.remove(customData);
    return ItemStack.isSameItemSameComponents(filterComponents, stackComponents);
  }

  @Override
  public void onNetworkEvent(Player player, int event) {
    if (event >= 0 && event <= 5 && this.defaultRoute != Util.ALL_DIRS[event]) {
      this.defaultRoute = Util.ALL_DIRS[event];
      java.util.Arrays.fill(this.batchRemaining, 0);
      this.setChanged();
    }
  }

  @Override
  public ContainerBase<?> createServerScreenHandler(int syncId, Player player) {
    return new ContainerSortingMachine(syncId, player.getInventory(), this);
  }

  @Override
  public ContainerBase<?> createClientScreenHandler(
      int syncId, Inventory inventory, GrowingBuffer data) {
    return new ContainerSortingMachine(syncId, inventory, this);
  }

  @Override
  public Set<UpgradableProperty> getUpgradableProperties() {
    return EnumSet.of(UpgradableProperty.Transformer);
  }

  @Override
  public void setChanged() {
    super.setChanged();
    if (IC2.sideProxy.isSimulating()) {
      this.setUpgradableBlock();
    }
  }

  public void setUpgradableBlock() {
    this.energy.setSinkTier(this.upgradeSlot.getTier(2));
  }

  @Override
  public double getEnergy() {
    return this.energy.getEnergy();
  }

  @Override
  public boolean useEnergy(double amount) {
    return this.energy.useEnergy(amount);
  }

  public ItemStack[] getFilterSlots(Direction side) {
    return this.filters[side.ordinal()];
  }
}
