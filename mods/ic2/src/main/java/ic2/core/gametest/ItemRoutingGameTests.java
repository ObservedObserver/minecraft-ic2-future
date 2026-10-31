package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.comp.Energy;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityItemBuffer;
import ic2.core.block.machine.tileentity.TileEntitySolidCanner;
import ic2.core.block.machine.tileentity.TileEntitySortingMachine;
import ic2.core.block.machine.tileentity.TileEntityWeightedItemDistributor;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class ItemRoutingGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);
  private static final BlockPos EAST_POS = new BlockPos(2, 1, 1);
  private static final BlockPos WEST_POS = new BlockPos(0, 1, 1);
  private static final BlockPos DOWN_POS = new BlockPos(1, 0, 1);

  // AE2 export buses insert through the sided NeoForge item-handler capability. Verify that both
  // canners expose that path and route tin cans into their dedicated can-input slots from every
  // possible attachment face.
  @GameTest(template = EMPTY)
  public static void cannersAcceptTinCansThroughSidedItemHandlers(GameTestHelper helper) {
    helper.setBlock(MACHINE_POS, Ic2Blocks.SOLID_CANNER);
    helper.setBlock(EAST_POS, Ic2Blocks.CANNER);
    TileEntitySolidCanner solidCanner = getTe(helper, MACHINE_POS, TileEntitySolidCanner.class);
    TileEntityCanner canner = getTe(helper, EAST_POS, TileEntityCanner.class);

    helper.assertValueEqual(
        canner.getMode(), TileEntityCanner.Mode.BottleSolid, "fluid/solid canner mode");
    assertAcceptsTinCanFromEverySide(helper, MACHINE_POS, "solid canner");
    assertAcceptsTinCanFromEverySide(helper, EAST_POS, "fluid/solid canner");

    helper.assertValueEqual(
        solidCanner.canInputSlot.get().getCount(),
        Direction.values().length,
        "tin cans in the solid canner can-input slot");
    helper.assertValueEqual(
        canner.canInputSlot.get().getCount(),
        Direction.values().length,
        "tin cans in the fluid/solid canner can-input slot");
    helper.succeed();
  }

  // sorting machine: items matching a side filter are sent to that side in filter-sized batches for
  // 20 EU per item
  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void sortingMachineRoutesFilteredItemsToConfiguredSide(GameTestHelper helper) {
    helper.setBlock(EAST_POS, Blocks.CHEST);
    helper.setBlock(DOWN_POS, Blocks.CHEST);
    helper.setBlock(MACHINE_POS, Ic2Blocks.SORTING_MACHINE);
    TileEntitySortingMachine te = getTe(helper, MACHINE_POS, TileEntitySortingMachine.class);
    te.getComponent(Energy.class).addEnergy(15000.0);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 16));

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(
              countItems(getTe(helper, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              16,
              "cobblestone routed to the east chest");
          helper.assertValueEqual(
              countItems(getTe(helper, DOWN_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              0,
              "no cobblestone may reach the default route");
        });
  }

  // items not matching any filter leave through the default route (down) one at a time
  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void sortingMachineSendsUnfilteredItemsToDefaultRoute(GameTestHelper helper) {
    helper.setBlock(EAST_POS, Blocks.CHEST);
    helper.setBlock(DOWN_POS, Blocks.CHEST);
    helper.setBlock(MACHINE_POS, Ic2Blocks.SORTING_MACHINE);
    TileEntitySortingMachine te = getTe(helper, MACHINE_POS, TileEntitySortingMachine.class);
    te.getComponent(Energy.class).addEnergy(15000.0);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.DIRT, 4));

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(
              countItems(getTe(helper, DOWN_POS, ChestBlockEntity.class), Items.DIRT),
              4,
              "dirt sent through the default route");
          helper.assertValueEqual(
              countItems(getTe(helper, EAST_POS, ChestBlockEntity.class), Items.DIRT),
              0,
              "no dirt may reach the filtered side");
        });
  }

  // item buffer: an ejector upgrade pushes the buffered items into an adjacent inventory one item
  // per tick
  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void itemBufferEjectsContentsIntoChest(GameTestHelper helper) {
    helper.setBlock(EAST_POS, Blocks.CHEST);
    helper.setBlock(MACHINE_POS, Ic2Blocks.ITEM_BUFFER);
    TileEntityItemBuffer te = getTe(helper, MACHINE_POS, TileEntityItemBuffer.class);
    te.upgradeSlot.put(0, new ItemStack(Ic2Items.EJECTOR_UPGRADE));
    te.rightcontentSlot.put(0, new ItemStack(Items.COBBLESTONE, 8));

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(
              countItems(getTe(helper, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              8,
              "cobblestone ejected into the chest");
          helper.assertTrue(
              te.rightcontentSlot.get(0).isEmpty(), "the buffer slot should be emptied");
        });
  }

  // weighted item distributor: the full buffer goes to the highest priority side that accepts it
  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void weightedItemDistributorPrefersFirstPriority(GameTestHelper helper) {
    TileEntityWeightedItemDistributor te = setupDistributor(helper);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 16));

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(
              countItems(getTe(helper, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              16,
              "cobblestone in the first priority chest");
          helper.assertValueEqual(
              countItems(getTe(helper, WEST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              0,
              "the second priority chest must stay empty");
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void weightedItemDistributorFallsBackWhenPriorityIsFull(GameTestHelper helper) {
    TileEntityWeightedItemDistributor te = setupDistributor(helper);
    ChestBlockEntity eastChest = getTe(helper, EAST_POS, ChestBlockEntity.class);

    for (int i = 0; i < eastChest.getContainerSize(); i++) {
      eastChest.setItem(i, new ItemStack(Items.DIRT, 64));
    }

    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 16));

    helper.succeedWhen(
        () -> {
          helper.assertValueEqual(
              countItems(getTe(helper, WEST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              16,
              "cobblestone must fall through to the second priority when the first is full");
        });
  }

  private static TileEntityWeightedItemDistributor setupDistributor(GameTestHelper helper) {
    helper.setBlock(EAST_POS, Blocks.CHEST);
    helper.setBlock(WEST_POS, Blocks.CHEST);
    // face the intake up so it does not collide with the east/west priorities
    helper.setBlock(
        MACHINE_POS,
        Ic2Blocks.WEIGHTED_ITEM_DISTRIBUTOR
            .defaultBlockState()
            .setValue(Ic2TileEntityBlock.anyFacingProperty, Direction.UP));
    TileEntityWeightedItemDistributor te =
        getTe(helper, MACHINE_POS, TileEntityWeightedItemDistributor.class);
    te.getPriority().add(Direction.EAST);
    te.getPriority().add(Direction.WEST);
    return te;
  }

  private static int countItems(Container container, Item item) {
    int total = 0;

    for (int i = 0; i < container.getContainerSize(); i++) {
      ItemStack stack = container.getItem(i);
      if (!stack.isEmpty() && stack.getItem() == item) {
        total += stack.getCount();
      }
    }

    return total;
  }

  private static void assertAcceptsTinCanFromEverySide(
      GameTestHelper helper, BlockPos pos, String machineName) {
    BlockPos absolutePos = helper.absolutePos(pos);
    for (Direction side : Direction.values()) {
      ResourceHandler<ItemResource> handler =
          helper.getLevel().getCapability(Capabilities.Item.BLOCK, absolutePos, side);
      helper.assertTrue(handler != null, machineName + " item handler missing on " + side);

      int before =
          countItems((Container) helper.getBlockEntity(pos, BlockEntity.class), Ic2Items.TIN_CAN);
      try (Transaction transaction = Transaction.openRoot()) {
        int accepted = handler.insert(ItemResource.of(Ic2Items.TIN_CAN), 128, transaction);
        helper.assertValueEqual(
            accepted, 64 - before, machineName + " capacity boundary from " + side);
      }
      helper.assertValueEqual(
          countItems((Container) helper.getBlockEntity(pos, BlockEntity.class), Ic2Items.TIN_CAN),
          before,
          machineName + " aborted insertion must restore inventory from " + side);
      try (Transaction transaction = Transaction.openRoot()) {
        int accepted = handler.insert(ItemResource.of(Ic2Items.TIN_CAN), 1, transaction);
        helper.assertValueEqual(accepted, 1, machineName + " accepted tin can from " + side);
        transaction.commit();
      }
    }
  }

  private static <T extends BlockEntity> T getTe(
      GameTestHelper helper, BlockPos pos, Class<T> type) {
    BlockEntity be =
        helper.getBlockEntity(pos, net.minecraft.world.level.block.entity.BlockEntity.class);
    if (!type.isInstance(be)) {
      throw new IllegalStateException(
          "expected " + type.getSimpleName() + " at " + pos + ", found " + be);
    }

    return type.cast(be);
  }

  @GameTest(template = EMPTY, batch = "sorting_audit", timeoutTicks = 180)
  public static void auditHopperFeedsSortingMachineAndBothRoutesWork(GameTestHelper h) {
    var te = auditSetup(h);
    var up = MACHINE_POS.above();
    h.setBlock(up, Blocks.HOPPER);
    var hopper = getTe(h, up, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
    hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 4));
    hopper.setItem(1, new ItemStack(Items.DIRT, 3));
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 1);
    h.succeedWhen(
        () -> {
          h.assertValueEqual(
              countItems(getTe(h, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
              4,
              "hopper stones filtered east");
          h.assertValueEqual(
              countItems(getTe(h, DOWN_POS, ChestBlockEntity.class), Items.DIRT),
              3,
              "hopper dirt defaults down");
          h.assertTrue(
              hopper.isEmpty() && te.buffer.isEmpty(),
              "all seven hopper items conserved and routed");
        });
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditInsertionCapabilityWorksOnAllSixSidesAndRollsBack(GameTestHelper h) {
    h.setBlock(MACHINE_POS, Ic2Blocks.SORTING_MACHINE);
    var te = getTe(h, MACHINE_POS, TileEntitySortingMachine.class);
    for (var side : Direction.values()) {
      var handler =
          h.getLevel().getCapability(Capabilities.Item.BLOCK, h.absolutePos(MACHINE_POS), side);
      h.assertTrue(handler != null, "capability exists on " + side);
      try (var transaction = Transaction.openRoot()) {
        h.assertValueEqual(
            handler.insert(ItemResource.of(Items.COBBLESTONE), 1, transaction),
            1,
            "accepts on " + side);
      }
      h.assertTrue(te.buffer.isEmpty(), "aborted insertion leaves buffer empty");
      try (var transaction = Transaction.openRoot()) {
        h.assertValueEqual(
            handler.insert(ItemResource.of(Items.COBBLESTONE), 1, transaction),
            1,
            "committed acceptance");
        transaction.commit();
      }
      h.assertValueEqual(countItems(te, Items.COBBLESTONE), 1, "one committed item");
      te.buffer.clear();
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditAdjacentChestAndGroundItemsAreNotAutomaticallySuckedIn(GameTestHelper h) {
    var te = auditSetup(h);
    var chest = getTe(h, EAST_POS, ChestBlockEntity.class);
    chest.setItem(0, new ItemStack(Items.COBBLESTONE, 5));
    var dropped =
        new net.minecraft.world.entity.item.ItemEntity(
            h.getLevel(),
            te.getBlockPos().getX() + .5,
            te.getBlockPos().getY() + 1.1,
            te.getBlockPos().getZ() + .5,
            new ItemStack(Items.DIRT, 3));
    h.getLevel().addFreshEntity(dropped);
    h.runAtTickTime(
        40,
        () -> {
          h.assertTrue(te.buffer.isEmpty(), "no automatic chest extraction or vacuum");
          h.assertValueEqual(
              countItems(chest, Items.COBBLESTONE), 5, "source chest retains all five");
          h.assertTrue(
              dropped.isAlive() && dropped.getItem().getCount() == 3, "ground items not absorbed");
          h.succeed();
        });
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditPowerAndBatchBoundaries(GameTestHelper h) throws Exception {
    var te = auditSetup(h);
    var energy = te.getComponent(Energy.class);
    energy.useEnergy(energy.getEnergy());
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 8));
    auditTick(te);
    h.assertValueEqual(countItems(te, Items.COBBLESTONE), 8, "unpowered sorter retains items");
    energy.addEnergy(159);
    auditTick(te);
    h.assertValueEqual(countItems(te, Items.COBBLESTONE), 8, "159EU cannot send eight-item batch");
    energy.addEnergy(1);
    auditTick(te);
    h.assertValueEqual(
        countItems(getTe(h, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
        8,
        "160EU sends eight-item batch");
    h.assertValueEqual((int) energy.getEnergy(), 0, "20EU charged per transferred item");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditUnderfilledBatchWaitsInsteadOfLeakingToDefault(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 7));
    auditTick(te);
    h.assertValueEqual(countItems(te, Items.COBBLESTONE), 7, "seven items wait for full batch");
    h.assertTrue(
        getTe(h, DOWN_POS, ChestBlockEntity.class).isEmpty(), "filtered items never default");
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 8));
    auditTick(te);
    h.assertValueEqual(
        countItems(getTe(h, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
        8,
        "completed batch routes");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditFullFilteredChestWaitsWithoutLossOrPowerCharge(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var chest = getTe(h, EAST_POS, ChestBlockEntity.class);
    for (int i = 0; i < chest.getContainerSize(); i++)
      chest.setItem(i, new ItemStack(Items.DIRT, 64));
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 8));
    double before = te.getEnergy();
    auditTick(te);
    h.assertValueEqual(countItems(te, Items.COBBLESTONE), 8, "blocked batch retained");
    h.assertValueEqual((int) te.getEnergy(), (int) before, "blocked attempt does not consume EU");
    h.assertTrue(
        getTe(h, DOWN_POS, ChestBlockEntity.class).isEmpty(),
        "blocked filter does not leak to default");
    chest.setItem(0, ItemStack.EMPTY);
    auditTick(te);
    h.assertValueEqual(countItems(chest, Items.COBBLESTONE), 8, "unblocked output resumes");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditBatchCanUseFreeSpaceAcrossMultipleDestinationSlots(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var chest = getTe(h, EAST_POS, ChestBlockEntity.class);
    chest.setItem(0, new ItemStack(Items.COBBLESTONE, 60));
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 8));
    auditTick(te);
    h.assertValueEqual(
        countItems(chest, Items.COBBLESTONE),
        68,
        "batch must fill four spaces then use next empty slot");
    h.assertTrue(te.buffer.isEmpty(), "all eight transferred");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditSelectingFormerFilteredSideAsDefaultDoesNotJam(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 1);
    te.onNetworkEvent(
        h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), Direction.EAST.ordinal());
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 1));
    auditTick(te);
    h.assertValueEqual(
        countItems(getTe(h, EAST_POS, ChestBlockEntity.class), Items.COBBLESTONE),
        1,
        "default route ignores its old filter row");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditModernItemComponentsRemainDistinctWhenFiltering(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var filter = new ItemStack(Items.DIRT);
    filter.set(
        net.minecraft.core.component.DataComponents.CUSTOM_NAME,
        net.minecraft.network.chat.Component.literal("A"));
    var input = new ItemStack(Items.DIRT);
    input.set(
        net.minecraft.core.component.DataComponents.CUSTOM_NAME,
        net.minecraft.network.chat.Component.literal("B"));
    te.getFilterSlots(Direction.EAST)[0] = filter;
    te.buffer.put(0, input);
    auditTick(te);
    h.assertValueEqual(
        countItems(getTe(h, EAST_POS, ChestBlockEntity.class), Items.DIRT),
        0,
        "renamed B must not match renamed A filter");
    h.assertValueEqual(
        countItems(getTe(h, DOWN_POS, ChestBlockEntity.class), Items.DIRT),
        1,
        "nonmatching components use default route");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditDefaultRouteNetworkChangeMarksChunkDirty(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var chunk = h.getLevel().getChunkAt(te.getBlockPos());
    auditClearDirty(chunk);
    te.onNetworkEvent(
        h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), Direction.NORTH.ordinal());
    h.assertTrue(
        chunk.isUnsaved(), "default-route GUI change must mark the owning chunk for saving");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditGhostFilterGuiChangeMarksChunkDirty(GameTestHelper h) throws Exception {
    var te = auditSetup(h);
    var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    var menu = te.createServerScreenHandler(7, player);
    var chunk = h.getLevel().getChunkAt(te.getBlockPos());
    auditClearDirty(chunk);
    menu.setCarried(new ItemStack(Items.COBBLESTONE, 1));
    int filterSlot = -1;
    for (int i = 0; i < menu.slots.size(); i++)
      if (menu.slots.get(i) instanceof ic2.core.slot.SlotHologramSlot) {
        filterSlot = i;
        break;
      }
    h.assertTrue(filterSlot >= 0, "GUI has filter slot");
    menu.clicked(filterSlot, 0, net.minecraft.world.inventory.ContainerInput.PICKUP, player);
    h.assertTrue(
        !te.getFilterSlots(Direction.DOWN)[0].isEmpty(), "GUI click actually changed ghost filter");
    h.assertTrue(chunk.isUnsaved(), "filter GUI change must mark owning chunk for saving");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void auditTransferAtChunkBorderMarksSourceChunkDirty(GameTestHelper h)
      throws Exception {
    var base = net.minecraft.world.level.ChunkPos.containing(h.absolutePos(MACHINE_POS));
    BlockPos machine = new BlockPos((base.x() + 1000) * 16 + 15, 80, (base.z() + 1000) * 16 + 8);
    BlockPos output = machine.east();
    h.getLevel().setBlockAndUpdate(machine, Ic2Blocks.SORTING_MACHINE.defaultBlockState());
    h.getLevel().setBlockAndUpdate(output, Blocks.CHEST.defaultBlockState());
    var te = (TileEntitySortingMachine) h.getLevel().getBlockEntity(machine);
    var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(output);
    te.getComponent(Energy.class).addEnergy(1000);
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 1));
    var chunk = h.getLevel().getChunkAt(machine);
    auditClearDirty(chunk);
    auditTick(te);
    h.assertValueEqual(
        countItems(chest, Items.COBBLESTONE), 1, "cross-border transfer really occurred");
    h.assertTrue(te.buffer.isEmpty(), "source item consumed in memory");
    h.assertTrue(
        chunk.isUnsaved(),
        "source inventory decrement must be persisted independently of destination chunk");
    h.succeed();
  }

  private static TileEntitySortingMachine auditSetup(GameTestHelper h) {
    h.setBlock(EAST_POS, Blocks.CHEST);
    h.setBlock(DOWN_POS, Blocks.CHEST);
    h.setBlock(MACHINE_POS, Ic2Blocks.SORTING_MACHINE);
    var te = getTe(h, MACHINE_POS, TileEntitySortingMachine.class);
    te.getComponent(Energy.class).addEnergy(15000);
    return te;
  }

  private static void auditTick(TileEntitySortingMachine te) throws Exception {
    var m = TileEntitySortingMachine.class.getDeclaredMethod("updateEntityServer");
    m.setAccessible(true);
    m.invoke(te);
  }

  private static void auditClearDirty(net.minecraft.world.level.chunk.ChunkAccess chunk)
      throws Exception {
    var field = net.minecraft.world.level.chunk.ChunkAccess.class.getDeclaredField("unsaved");
    field.setAccessible(true);
    field.setBoolean(chunk, false);
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void batchWithInsufficientTotalSpaceDoesNotPartiallyCommit(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var chest = getTe(h, EAST_POS, ChestBlockEntity.class);
    for (int i = 0; i < chest.getContainerSize(); i++)
      chest.setItem(i, new ItemStack(Items.DIRT, 64));
    chest.setItem(0, new ItemStack(Items.COBBLESTONE, 60));
    te.getFilterSlots(Direction.EAST)[0] = new ItemStack(Items.COBBLESTONE, 8);
    te.buffer.put(0, new ItemStack(Items.COBBLESTONE, 8));
    double energy = te.getEnergy();
    auditTick(te);
    h.assertValueEqual(
        countItems(chest, Items.COBBLESTONE),
        60,
        "simulation must not insert the four-item partial capacity");
    h.assertValueEqual(countItems(te, Items.COBBLESTONE), 8, "entire batch retained");
    h.assertValueEqual((int) te.getEnergy(), (int) energy, "no energy charged for rejected batch");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void potionContentsAndToolDamageHaveDistinctRoutes(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var healing =
        net.minecraft.world.item.alchemy.PotionContents.createItemStack(
            Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING);
    var water =
        net.minecraft.world.item.alchemy.PotionContents.createItemStack(
            Items.POTION, net.minecraft.world.item.alchemy.Potions.WATER);
    var pristine = new ItemStack(Items.IRON_PICKAXE);
    var damaged = new ItemStack(Items.IRON_PICKAXE);
    damaged.setDamageValue(7);
    te.getFilterSlots(Direction.EAST)[0] = healing;
    te.getFilterSlots(Direction.EAST)[1] = pristine;
    te.buffer.put(0, water.copy());
    te.buffer.put(1, damaged.copy());
    te.buffer.put(2, healing.copy());
    te.buffer.put(3, pristine.copy());
    auditTick(te);
    var east = getTe(h, EAST_POS, ChestBlockEntity.class);
    var down = getTe(h, DOWN_POS, ChestBlockEntity.class);
    h.assertTrue(
        ItemStack.isSameItemSameComponents(east.getItem(0), healing),
        "healing potion matches its component filter");
    h.assertTrue(
        ItemStack.isSameItemSameComponents(down.getItem(0), water),
        "water potion must take default route");
    h.assertTrue(
        east.getItem(1).is(Items.IRON_PICKAXE) && east.getItem(1).getDamageValue() == 0,
        "undamaged pickaxe filtered");
    h.assertTrue(
        down.getItem(1).is(Items.IRON_PICKAXE) && down.getItem(1).getDamageValue() == 7,
        "damaged pickaxe takes default route");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void legacyChargeExemptionAndNonExemptCustomDataArePreserved(GameTestHelper h)
      throws Exception {
    var te = auditSetup(h);
    var filter = new ItemStack(Items.DIRT);
    var input = new ItemStack(Items.DIRT);
    var other = new ItemStack(Items.DIRT);
    var a = new net.minecraft.nbt.CompoundTag();
    a.putInt("charge", 0);
    a.putString("mode", "A");
    var b = a.copy();
    b.putInt("charge", 40);
    var c = b.copy();
    c.putString("mode", "B");
    filter.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(a));
    input.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(b));
    other.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(c));
    te.getFilterSlots(Direction.EAST)[0] = filter;
    te.buffer.put(0, input);
    te.buffer.put(1, other);
    auditTick(te);
    h.assertValueEqual(
        countItems(getTe(h, EAST_POS, ChestBlockEntity.class), Items.DIRT),
        1,
        "different charge remains matchable");
    h.assertValueEqual(
        countItems(getTe(h, DOWN_POS, ChestBlockEntity.class), Items.DIRT),
        1,
        "different non-exempt custom data remains distinct");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "sorting_audit")
  public static void filterComponentsAndSparseRulesSurviveNbtReload(GameTestHelper h) {
    var te = auditSetup(h);
    var potion =
        net.minecraft.world.item.alchemy.PotionContents.createItemStack(
            Items.POTION, net.minecraft.world.item.alchemy.Potions.HEALING);
    te.getFilterSlots(Direction.EAST)[0] = potion;
    te.defaultRoute = Direction.WEST;
    var tag = te.saveWithFullMetadata(h.getLevel().registryAccess());
    te.getFilterSlots(Direction.NORTH)[3] = new ItemStack(Items.DIRT);
    te.loadWithComponents(tag, h.getLevel().registryAccess());
    h.assertTrue(
        ItemStack.isSameItemSameComponents(te.getFilterSlots(Direction.EAST)[0], potion),
        "registered potion component serialized with the provided registry context");
    h.assertTrue(
        ic2.core.util.StackUtil.isEmpty(te.getFilterSlots(Direction.NORTH)[3]),
        "reloading sparse NBT clears obsolete rules");
    h.assertTrue(te.defaultRoute == Direction.WEST, "default route persisted");
    h.succeed();
  }
}
