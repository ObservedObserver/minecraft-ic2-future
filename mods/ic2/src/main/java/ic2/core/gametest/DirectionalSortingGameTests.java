package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityItemBuffer;
import ic2.core.block.machine.tileentity.TileEntitySortingMachine;
import ic2.core.item.upgrade.ItemUpgradeModule;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import ic2.core.slot.SlotHologramSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class DirectionalSortingGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  private static net.minecraft.world.level.block.entity.BlockEntity blockEntity(
      GameTestHelper h, BlockPos pos) {
    return h.getLevel().getBlockEntity(h.absolutePos(pos));
  }

  private static TileEntitySortingMachine machine(GameTestHelper h, Direction input) {
    h.setBlock(POS, Ic2Blocks.SORTING_MACHINE);
    var te = (TileEntitySortingMachine) blockEntity(h, POS);
    te.defaultRoute = input;
    te.getComponent(Energy.class).addEnergy(15000);
    te.getFilterSlots(input)[0] = new ItemStack(Items.IRON_INGOT);
    return te;
  }

  private static ChestBlockEntity chest(GameTestHelper h, Direction side) {
    h.setBlock(POS.relative(side), Blocks.CHEST);
    return (ChestBlockEntity) blockEntity(h, POS.relative(side));
  }

  private static void tick(TileEntitySortingMachine te) throws Exception {
    var method = TileEntitySortingMachine.class.getDeclaredMethod("updateEntityServer");
    method.setAccessible(true);
    method.invoke(te);
  }

  private static int count(Container container, Item item) {
    int n = 0;
    for (int i = 0; i < container.getContainerSize(); i++) {
      var stack = container.getItem(i);
      if (stack.is(item)) n += stack.getCount();
    }
    return n;
  }

  @GameTest(template = EMPTY)
  public static void selectedInputWorksOnAllSixFaces(GameTestHelper h) throws Exception {
    for (Direction input : Direction.values()) {
      for (Direction side : Direction.values()) h.setBlock(POS.relative(side), Blocks.AIR);
      var te = machine(h, input);
      var supply = chest(h, input);
      var output = chest(h, input.getOpposite());
      te.getFilterSlots(input.getOpposite())[0] = new ItemStack(Items.IRON_INGOT);
      supply.setItem(0, new ItemStack(Items.IRON_INGOT, 3));
      for (int i = 1; i <= 3; i++) {
        tick(te);
        h.assertValueEqual(
            count(supply, Items.IRON_INGOT), 3 - i, "source decreases once on " + input);
        h.assertValueEqual(count(output, Items.IRON_INGOT), i, "one item per tick on " + input);
      }
      h.assertTrue(te.buffer.isEmpty(), "all routed on " + input);
      h.assertValueEqual((int) te.getEnergy(), 14940, "only output charged on " + input);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 100)
  public static void naturalServerTicksPullSelectedWestAndOutputUp(GameTestHelper h) {
    var te = machine(h, Direction.WEST);
    var supply = chest(h, Direction.WEST);
    var target = chest(h, Direction.UP);
    supply.setItem(0, new ItemStack(Items.IRON_INGOT, 4));
    te.getFilterSlots(Direction.UP)[0] = new ItemStack(Items.IRON_INGOT);
    h.succeedWhen(
        () -> {
          h.assertTrue(supply.isEmpty() && te.buffer.isEmpty(), "source and buffer empty");
          h.assertValueEqual(count(target, Items.IRON_INGOT), 4, "up is an ordinary output");
          h.assertValueEqual((int) te.getEnergy(), 14920, "four outputs cost80");
        });
  }

  @GameTest(template = EMPTY)
  public static void inputWhitelistSkipsOtherItemsAndEmptyDisablesInput(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.UP);
    var supply = chest(h, Direction.UP);
    supply.setItem(0, new ItemStack(Items.DIRT, 64));
    supply.setItem(1, new ItemStack(Items.IRON_INGOT, 2));
    tick(te);
    h.assertValueEqual(count(supply, Items.DIRT), 64, "unlisted source retained");
    h.assertValueEqual(count(te, Items.IRON_INGOT), 1, "later matching slot pulled");
    te.getFilterSlots(Direction.UP)[0] = ItemStack.EMPTY;
    tick(te);
    h.assertValueEqual(count(supply, Items.IRON_INGOT), 1, "empty input disables pull");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void noFallbackAndInputNeverOutputs(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.NORTH);
    var supply = chest(h, Direction.NORTH);
    var east = chest(h, Direction.EAST);
    te.buffer.put(0, new ItemStack(Items.IRON_INGOT, 4));
    te.buffer.put(1, new ItemStack(Items.DIRT, 3));
    for (int i = 0; i < 5; i++) tick(te);
    h.assertTrue(east.isEmpty() && supply.isEmpty(), "no empty whitelist or D fallback output");
    h.assertValueEqual(
        count(te, Items.IRON_INGOT), 4, "input whitelist is not an output whitelist");
    h.assertValueEqual(count(te, Items.DIRT), 3, "unroutable item retained");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void batchesWaitThenDrainOnePerTickAcrossBufferSlots(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT, 4);
    te.buffer.put(0, new ItemStack(Items.IRON_INGOT, 2));
    te.buffer.put(1, new ItemStack(Items.IRON_INGOT));
    tick(te);
    h.assertTrue(target.isEmpty(), "underfilled batch waits");
    te.buffer.put(1, new ItemStack(Items.IRON_INGOT, 2));
    for (int i = 1; i <= 4; i++) {
      tick(te);
      h.assertValueEqual(count(target, Items.IRON_INGOT), i, "no burst and no stuck batch tail");
    }
    h.assertTrue(te.buffer.isEmpty(), "batch tail drained across slots");
    h.assertValueEqual((int) te.getEnergy(), 14920, "batch charged exactly80");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void fullOutputRollsBackAndFullBufferStopsPull(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.UP);
    var source = chest(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT);
    for (int i = 0; i < target.getContainerSize(); i++)
      target.setItem(i, new ItemStack(Items.DIRT, 64));
    for (int i = 0; i < te.buffer.size(); i++)
      te.buffer.put(i, new ItemStack(Items.IRON_INGOT, 64));
    source.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
    tick(te);
    h.assertValueEqual(count(source, Items.IRON_INGOT), 2, "full buffer did not remove source");
    h.assertValueEqual(
        count(te, Items.IRON_INGOT), 704, "failed destination transaction restores buffer");
    h.assertValueEqual((int) te.getEnergy(), 15000, "blocked operation free");
    target.setItem(0, new ItemStack(Items.IRON_INGOT, 63));
    tick(te);
    h.assertValueEqual(count(target, Items.IRON_INGOT), 64, "one free space used once");
    tick(te);
    h.assertValueEqual(
        count(source, Items.IRON_INGOT)
            + count(te, Items.IRON_INGOT)
            + count(target, Items.IRON_INGOT),
        769,
        "source+buffer+target conserved");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void powerBoundaryAndComponentWhitelist(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.UP);
    var source = chest(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    var named = new ItemStack(Items.IRON_INGOT);
    named.set(DataComponents.CUSTOM_NAME, Component.literal("sample"));
    source.setItem(0, new ItemStack(Items.IRON_INGOT));
    source.setItem(1, named.copy());
    te.getFilterSlots(Direction.UP)[0] = named.copy();
    te.getFilterSlots(Direction.EAST)[0] = named.copy();
    var energy = te.getComponent(Energy.class);
    energy.useEnergy(energy.getEnergy());
    energy.addEnergy(19);
    tick(te);
    h.assertValueEqual(count(source, Items.IRON_INGOT), 2, "19EU cannot pull");
    energy.addEnergy(1);
    tick(te);
    h.assertValueEqual(count(source, Items.IRON_INGOT), 1, "only named stack pulled");
    h.assertTrue(
        ItemStack.isSameItemSameComponents(target.getItem(0), named), "components retained");
    h.assertValueEqual((int) te.getEnergy(), 0, "one output costs20");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void activeBatchAndInputSelectionSurviveNbtReload(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.NORTH);
    var target = chest(h, Direction.UP);
    te.getFilterSlots(Direction.UP)[0] = new ItemStack(Items.IRON_INGOT, 4);
    te.buffer.put(0, new ItemStack(Items.IRON_INGOT, 4));
    tick(te);
    var tag = te.saveWithFullMetadata(h.getLevel().registryAccess());
    var restored =
        new TileEntitySortingMachine(
            h.absolutePos(POS), Ic2Blocks.SORTING_MACHINE.defaultBlockState());
    restored.setLevel(h.getLevel());
    restored.loadWithComponents(tag, h.getLevel().registryAccess());
    h.assertTrue(restored.defaultRoute == Direction.NORTH, "D selection retained");
    for (int i = 0; i < 3; i++) tick(restored);
    h.assertValueEqual(count(target, Items.IRON_INGOT), 4, "remaining batch survives reload");
    h.assertTrue(restored.buffer.isEmpty(), "tail drains without requiring another full batch");
    h.assertValueEqual((int) restored.getEnergy(), 14920, "no double charge on reload");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void networkSelectionAndAllGhostRowsRemainEditable(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.UP);
    var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    var menu = te.createServerScreenHandler(7, player);
    int ghosts = 0;
    for (var slot : menu.slots) if (slot instanceof SlotHologramSlot) ghosts++;
    h.assertValueEqual(ghosts, 42, "all six rows are active ghost filters");
    for (Direction side : Direction.values()) {
      te.onNetworkEvent(player, side.ordinal());
      h.assertTrue(te.defaultRoute == side, "D changes to " + side);
      int idx = -1;
      for (int i = 0; i < menu.slots.size(); i++) {
        var slot = menu.slots.get(i);
        if (slot instanceof SlotHologramSlot && slot.x == 80 && slot.y == 19 + side.ordinal() * 20)
          idx = i;
      }
      h.assertTrue(idx >= 0, "ghost row exists");
      menu.setCarried(new ItemStack(Items.IRON_INGOT));
      menu.clicked(idx, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
      h.assertTrue(
          te.getFilterSlots(side)[0].is(Items.IRON_INGOT), "production click updated rule");
      h.assertValueEqual(menu.getCarried().getCount(), 1, "ghost click consumes no item");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void sideCapabilitiesEnforceInputAndOutputWhitelists(GameTestHelper h) {
    var te = machine(h, Direction.EAST);
    te.getFilterSlots(Direction.UP)[0] = new ItemStack(Items.IRON_INGOT);
    for (Direction side : Direction.values()) {
      var handler = h.getLevel().getCapability(Capabilities.Item.BLOCK, h.absolutePos(POS), side);
      h.assertTrue(handler != null, "item capability exposed");
      try (var tx = Transaction.openRoot()) {
        int accepted = handler.insert(ItemResource.of(Items.IRON_INGOT), 1, tx);
        h.assertValueEqual(accepted, side == Direction.EAST ? 1 : 0, "only D can receive");
        h.assertValueEqual(
            handler.insert(ItemResource.of(Items.DIRT), 1, tx), 0, "input whitelist enforced");
      }
      h.assertTrue(te.buffer.isEmpty(), "simulation rolled back");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void outputRateMatchesSingleOrdinaryEjector(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT);
    for (int i = 0; i < te.buffer.size(); i++) te.buffer.put(i, new ItemStack(Items.IRON_INGOT));
    var referencePos = new BlockPos(0, 1, 0);
    var referenceTargetPos = new BlockPos(0, 1, 1);
    h.setBlock(referencePos, Ic2Blocks.ITEM_BUFFER);
    h.setBlock(referenceTargetPos, Blocks.CHEST);
    var reference = (TileEntityItemBuffer) blockEntity(h, referencePos);
    var referenceTarget = (ChestBlockEntity) blockEntity(h, referenceTargetPos);
    reference.rightcontentSlot.put(0, new ItemStack(Items.IRON_INGOT, 11));
    var upgrade = new ItemStack(Ic2Items.EJECTOR_UPGRADE);
    for (int i = 1; i <= 11; i++) {
      tick(te);
      ((ItemUpgradeModule) upgrade.getItem()).onTick(upgrade, reference);
      h.assertValueEqual(
          count(target, Items.IRON_INGOT), i, "all buffer slots share one per-face quota");
      h.assertValueEqual(
          count(referenceTarget, Items.IRON_INGOT), i, "same per-tick result as ordinary ejector");
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void barrelInputAndOutputUseInventoryCapabilities(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.NORTH);
    h.setBlock(POS.north(), Blocks.BARREL);
    h.setBlock(POS.east(), Blocks.BARREL);
    var source = (Container) blockEntity(h, POS.north());
    var target = (Container) blockEntity(h, POS.east());
    source.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT);
    tick(te);
    tick(te);
    h.assertTrue(source.isEmpty() && te.buffer.isEmpty(), "barrel supply consumed exactly once");
    h.assertValueEqual(count(target, Items.IRON_INGOT), 2, "barrel output accepted");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void outputUsesOppositeContactFaceOfSidedContainer(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.WEST);
    h.setBlock(POS.below(), Blocks.FURNACE);
    h.setBlock(POS.above(), Blocks.FURNACE);
    te.getFilterSlots(Direction.DOWN)[0] = new ItemStack(Items.RAW_IRON);
    te.getFilterSlots(Direction.UP)[0] = new ItemStack(Items.RAW_GOLD);
    te.buffer.put(0, new ItemStack(Items.RAW_IRON));
    te.buffer.put(1, new ItemStack(Items.RAW_GOLD));
    tick(te);
    h.assertValueEqual(
        count((Container) blockEntity(h, POS.below()), Items.RAW_IRON),
        1,
        "lower furnace accepts through its TOP input");
    h.assertTrue(
        ((Container) blockEntity(h, POS.above())).isEmpty(),
        "upper furnace BOTTOM rejects raw material");
    h.assertValueEqual(count(te, Items.RAW_GOLD), 1, "rejected transfer rolled back");
    h.assertValueEqual((int) te.getEnergy(), 14980, "only accepted item charged");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void changingRuleOrInputCancelsOldBatchBeforeFurtherOutput(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.WEST);
    var target = chest(h, Direction.EAST);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT, 4);
    te.buffer.put(0, new ItemStack(Items.IRON_INGOT, 4));
    tick(te);
    te.getFilterSlots(Direction.EAST)[0] = ItemStack.EMPTY;
    tick(te);
    h.assertValueEqual(
        count(target, Items.IRON_INGOT), 1, "clearing whitelist stops pending output immediately");
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT, 1);
    te.onNetworkEvent(
        h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), Direction.EAST.ordinal());
    tick(te);
    h.assertValueEqual(count(target, Items.IRON_INGOT), 0, "new D pulls from the former output");
    h.assertValueEqual(
        count(te, Items.IRON_INGOT), 4, "input face cannot emit its old pending batch");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void sparseRulesAndComponentsRemainDistinctOnReload(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.WEST);
    var target = chest(h, Direction.EAST);
    var damaged = new ItemStack(Items.IRON_PICKAXE);
    damaged.setDamageValue(8);
    te.getFilterSlots(Direction.EAST)[6] = damaged.copy();
    var tag = te.saveWithFullMetadata(h.getLevel().registryAccess());
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT);
    te.loadWithComponents(tag, h.getLevel().registryAccess());
    h.assertTrue(te.getFilterSlots(Direction.EAST)[0].isEmpty(), "sparse reload clears stale rule");
    te.buffer.put(0, new ItemStack(Items.IRON_PICKAXE));
    te.buffer.put(1, damaged.copy());
    tick(te);
    h.assertValueEqual(count(target, Items.IRON_PICKAXE), 1, "exact damaged tool exported");
    h.assertValueEqual(target.getItem(0).getDamageValue(), 8, "damage component retained");
    h.assertValueEqual(te.buffer.get(0).getDamageValue(), 0, "pristine tool retained");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void rejectedFirstCandidateDoesNotBlockOtherWhitelistedItem(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    for (int i = 0; i < target.getContainerSize(); i++)
      target.setItem(i, new ItemStack(Items.DIRT, 64));
    target.setItem(0, new ItemStack(Items.GOLD_INGOT, 63));
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT, 4);
    te.getFilterSlots(Direction.EAST)[1] = new ItemStack(Items.GOLD_INGOT);
    te.buffer.put(0, new ItemStack(Items.IRON_INGOT, 4));
    te.buffer.put(1, new ItemStack(Items.GOLD_INGOT));
    tick(te);
    h.assertValueEqual(
        count(target, Items.GOLD_INGOT), 64, "later whitelist item uses matching free space");
    h.assertValueEqual(count(te, Items.IRON_INGOT), 4, "rejected batch intact");
    h.assertValueEqual((int) te.getEnergy(), 14980, "only accepted item charged");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void externallyRemovedBatchDoesNotJamOtherItems(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT, 4);
    te.getFilterSlots(Direction.EAST)[1] = new ItemStack(Items.GOLD_INGOT);
    te.buffer.put(0, new ItemStack(Items.IRON_INGOT, 4));
    tick(te);
    te.buffer.clear(0);
    te.buffer.put(1, new ItemStack(Items.GOLD_INGOT));
    tick(te);
    h.assertValueEqual(count(target, Items.IRON_INGOT), 1, "original batch first item sent");
    h.assertValueEqual(
        count(target, Items.GOLD_INGOT), 1, "missing old batch cannot block next rule");
    h.assertValueEqual((int) te.getEnergy(), 14960, "only two actual exports charged");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void potionWhitelistDistinguishesContents(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.UP);
    var source = chest(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    var healing =
        net.minecraft.world.item.alchemy.PotionContents.createItemStack(
            Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING);
    var water =
        net.minecraft.world.item.alchemy.PotionContents.createItemStack(
            Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
    te.getFilterSlots(Direction.UP)[0] = healing.copy();
    te.getFilterSlots(Direction.EAST)[0] = healing.copy();
    source.setItem(0, water.copy());
    source.setItem(1, healing.copy());
    tick(te);
    h.assertTrue(
        ItemStack.isSameItemSameComponents(source.getItem(0), water),
        "unlisted potion remains at source");
    h.assertTrue(
        ItemStack.isSameItemSameComponents(target.getItem(0), healing),
        "only listed potion transferred");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void customDataKeepsChargeExemptionAndRejectsOtherKeys(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.UP);
    var source = chest(h, Direction.UP);
    var target = chest(h, Direction.EAST);
    var a = new net.minecraft.nbt.CompoundTag();
    a.putInt("charge", 0);
    a.putString("mode", "A");
    var b = a.copy();
    b.putInt("charge", 40);
    var c = b.copy();
    c.putString("mode", "B");
    var filter = new ItemStack(Items.DIRT);
    filter.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(a));
    var charged = new ItemStack(Items.DIRT);
    charged.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(b));
    var other = new ItemStack(Items.DIRT);
    other.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(c));
    te.getFilterSlots(Direction.UP)[0] = filter.copy();
    te.getFilterSlots(Direction.EAST)[0] = filter.copy();
    source.setItem(0, charged.copy());
    source.setItem(1, other.copy());
    tick(te);
    h.assertTrue(
        ItemStack.isSameItemSameComponents(target.getItem(0), charged),
        "charge ignored for matching but preserved in transfer");
    h.assertTrue(
        ItemStack.isSameItemSameComponents(source.getItem(1), other),
        "nonexempt mode remains distinct");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void doubleChestOtherHalfSuppliesWhitelistedItems(GameTestHelper h)
      throws Exception {
    var te = machine(h, Direction.UP);
    var left =
        Blocks.CHEST
            .defaultBlockState()
            .setValue(net.minecraft.world.level.block.ChestBlock.FACING, Direction.NORTH)
            .setValue(
                net.minecraft.world.level.block.ChestBlock.TYPE,
                net.minecraft.world.level.block.state.properties.ChestType.LEFT);
    h.setBlock(POS.above(), left);
    h.setBlock(
        POS.above().east(),
        left.setValue(
            net.minecraft.world.level.block.ChestBlock.TYPE,
            net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
    var other = (ChestBlockEntity) blockEntity(h, POS.above().east());
    var target = chest(h, Direction.EAST);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.IRON_INGOT);
    other.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
    tick(te);
    tick(te);
    h.assertTrue(other.isEmpty(), "combined capability reaches other chest half");
    h.assertValueEqual(count(target, Items.IRON_INGOT), 2, "both ingots delivered");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void inputRespectsSourceExtractionPermissions(GameTestHelper h) throws Exception {
    var te = machine(h, Direction.UP);
    h.setBlock(POS.above(), Blocks.FURNACE);
    var source = (Container) blockEntity(h, POS.above());
    source.setItem(0, new ItemStack(Items.RAW_IRON));
    te.getFilterSlots(Direction.UP)[0] = new ItemStack(Items.RAW_IRON);
    tick(te);
    h.assertValueEqual(
        count(source, Items.RAW_IRON), 1, "cannot pull input slot through furnace bottom");
    h.assertTrue(te.buffer.isEmpty(), "forbidden extraction leaves buffer empty");
    h.assertValueEqual((int) te.getEnergy(), 15000, "rejected input costs no output energy");
    h.succeed();
  }
}
