package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityItemBuffer;
import ic2.core.block.machine.tileentity.TileEntitySolidCanner;
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
}
