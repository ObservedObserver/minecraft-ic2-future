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
import ic2.core.item.EnvItemHandler;
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

@NotClassic
public class TileEntitySortingMachine extends TileEntityElectricMachine
    implements IHasGui, INetworkClientTileEntityEventListener, IUpgradableBlock {
  public static final int defaultTier = 2;
  public final InvSlotUpgrade upgradeSlot;
  public final InvSlot buffer;
  private final ItemStack[][] filters;
  public Direction defaultRoute = Direction.DOWN;

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

    boolean changed = false;
    slots:
    for (int index = 0; index < this.buffer.size(); index++) {
      if (this.energy.getEnergy() < 20.0) {
        break;
      }
      ItemStack stack = this.buffer.get(index);
      if (StackUtil.isEmpty(stack)) {
        continue;
      }
      for (EnvItemHandler.AdjacentInventory inv : StackUtil.ENV.getAdjacentInventories(this)) {
        if (inv.getSide() == this.defaultRoute) {
          if (!this.matchesFilteredRoute(stack)) {
            int amount = StackUtil.ENV.deposit(inv, StackUtil.copyWithSize(stack, 1), false);
            if (amount > 0) {
              this.buffer.put(index, StackUtil.decSize(stack, amount));
              this.energy.useEnergy(amount * 20.0);
              changed = true;
            }
            continue slots;
          }
        } else {
          for (ItemStack filter : this.getFilterSlots(inv.getSide())) {
            if (matchesFilter(filter, stack)
                && stack.getCount() >= filter.getCount()
                && this.energy.canUseEnergy(filter.getCount() * 20.0)) {
              ItemStack batch = stack.copyWithCount(filter.getCount());
              // A short simulation must leave both inventories and the energy unchanged.
              if (StackUtil.ENV.deposit(inv, batch, true) == batch.getCount()) {
                int amount = StackUtil.ENV.deposit(inv, batch, false);
                if (amount > 0) {
                  stack = StackUtil.decSize(stack, amount);
                  this.buffer.put(index, stack);
                  this.energy.useEnergy(amount * 20.0);
                  changed = true;
                  if (StackUtil.isEmpty(stack)) {
                    continue slots;
                  }
                }
              }
              break;
            }
          }
        }
      }
    }
    if (changed) {
      this.setChanged();
    }
  }

  private boolean matchesFilteredRoute(ItemStack stack) {
    for (Direction side : Util.ALL_DIRS) {
      if (side == this.defaultRoute) {
        continue;
      }
      for (ItemStack filter : this.getFilterSlots(side)) {
        if (matchesFilter(filter, stack)) {
          return true;
        }
      }
    }
    return false;
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
