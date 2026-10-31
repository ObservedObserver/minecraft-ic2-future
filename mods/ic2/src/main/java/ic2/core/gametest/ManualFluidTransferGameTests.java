package ic2.core.gametest;

import com.mojang.authlib.GameProfile;
import ic2.compat.GameTest;
import ic2.core.block.machine.container.ContainerSteamGenerator;
import ic2.core.block.machine.tileentity.TileEntitySteamGenerator;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Fluids;
import ic2.core.ref.Ic2Items;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native player interaction tests, including event registration and item-use fallthrough. */
public class ManualFluidTransferGameTests {
  private static final String EMPTY = "gametest/empty7x7x7";
  private static final BlockPos POS = new BlockPos(2, 1, 3);

  private static TileEntitySteamGenerator boiler(GameTestHelper helper) {
    helper.setBlock(POS, Ic2Blocks.STEAM_GENERATOR);
    return (TileEntitySteamGenerator) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
  }

  private static ServerPlayer player(GameTestHelper helper) {
    UUID id = UUID.randomUUID();
    var profile = new GameProfile(id, "fill_" + id.toString().replace("-", "").substring(0, 11));
    var cookie = CommonListenerCookie.createInitial(profile, false);
    var server = helper.getLevel().getServer();
    var player = new ServerPlayer(server, helper.getLevel(), profile, cookie.clientInformation());
    var connection = new Connection(PacketFlow.SERVERBOUND);
    new EmbeddedChannel(connection);
    server.getPlayerList().placeNewPlayer(connection, player, cookie);
    // Headless transport, as in native GameTest mocks; menu creation still executes normally.
    player.connection = new ServerGamePacketListenerImpl(server, connection, player, cookie) {
      @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
      @Override public void send(net.minecraft.network.protocol.Packet<?> packet,
          io.netty.channel.ChannelFutureListener listener) {}
    };
    player.setGameMode(GameType.SURVIVAL);
    player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
    helper.assertValueEqual(player.gameMode(), GameType.SURVIVAL, "survival fixture");
    return player;
  }

  private static InteractionResult click(GameTestHelper helper, ServerPlayer player,
      InteractionHand hand, ItemStack stack, Direction side, boolean sneak) {
    BlockPos pos = helper.absolutePos(POS);
    player.setItemInHand(hand, stack);
    player.setShiftKeyDown(sneak);
    player.setPos(pos.getX() + 0.5, pos.getY() + 0.5 - player.getEyeHeight(), pos.getZ() - 2.0);
    player.setYRot(0.0F);
    player.setXRot(0.0F);
    Vec3 hitPosition = Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5));
    var hit = new BlockHitResult(hitPosition, side, pos, false);
    var result = player.gameMode.useItemOn(player, helper.getLevel(), stack, hand, hit);
    // FAIL stops the client action, too. Only PASS follows through to using a bucket in air.
    if (result == InteractionResult.PASS) {
      result = player.gameMode.useItem(player, helper.getLevel(), player.getItemInHand(hand), hand);
    }
    return result;
  }

  private static InteractionResult fill(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
    return click(helper, player, InteractionHand.MAIN_HAND, stack, Direction.NORTH, true);
  }

  private static int count(ServerPlayer player, Item item) {
    int count = 0;
    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
      ItemStack stack = player.getInventory().getItem(i);
      if (stack.is(item)) count += stack.getCount();
    }
    return count;
  }

  private static void noSpill(GameTestHelper helper) {
    for (Direction side : Direction.values()) {
      helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(POS.relative(side))).isEmpty(),
          "fluid must not be placed outside the boiler: " + side);
    }
  }

  @GameTest(template = EMPTY)
  public static void waterBucketFillsThroughNativeEventOnAllSixSides(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    for (Direction side : Direction.values()) {
      player.getInventory().clearContent();
      boiler.waterTank.drainMbUnchecked(10000, false);
      var result = click(helper, player, InteractionHand.MAIN_HAND,
          new ItemStack(Items.WATER_BUCKET), side, true);
      helper.assertTrue(result.consumesAction(), "bucket fill must consume interaction");
      helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "water inserted on " + side);
      helper.assertValueEqual(count(player, Items.BUCKET), 1, "one empty bucket returned");
      helper.assertValueEqual(count(player, Items.WATER_BUCKET), 0, "filled bucket consumed");
      noSpill(helper);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void waterAndDistilledCellsFillOnAllSixSides(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    for (Item item : List.of(Ic2Items.WATER_CELL, Ic2Items.DISTILLED_WATER_CELL)) {
      for (Direction side : Direction.values()) {
        player.getInventory().clearContent();
        boiler.waterTank.drainMbUnchecked(10000, false);
        click(helper, player, InteractionHand.MAIN_HAND, new ItemStack(item), side, true);
        helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "cell water inserted");
        helper.assertTrue(boiler.waterTank.getFluidStack().getFluid()
            == (item == Ic2Items.WATER_CELL ? Fluids.WATER : Ic2Fluids.DISTILLED_WATER.still()),
            "correct water type retained");
        helper.assertValueEqual(count(player, Ic2Items.EMPTY_CELL), 1, "empty cell returned");
        noSpill(helper);
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void stackedCellsTransferExactlyOnePerClick(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Ic2Items.WATER_CELL, 3));
    for (int i = 1; i <= 3; i++) {
      fill(helper, player, player.getMainHandItem());
      helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000 * i, "one cell per click");
      helper.assertValueEqual(count(player, Ic2Items.WATER_CELL), 3 - i, "remaining filled cells");
      helper.assertValueEqual(count(player, Ic2Items.EMPTY_CELL), i, "returned empty cells");
    }
    noSpill(helper);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void offhandFillLeavesMainHandUntouched(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 3));
    click(helper, player, InteractionHand.OFF_HAND, new ItemStack(Items.WATER_BUCKET), Direction.NORTH, true);
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "offhand water");
    helper.assertTrue(player.getOffhandItem().is(Items.BUCKET), "empty bucket in offhand");
    helper.assertTrue(player.getMainHandItem().is(Items.STICK)
        && player.getMainHandItem().getCount() == 3, "main hand unchanged");
    noSpill(helper);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void fullTankRejectsWithoutConsumptionOrSpill(GameTestHelper helper) {
    var boiler = boiler(helper);
    boiler.waterTank.fillMb(Ic2FluidStack.create(Fluids.WATER, 10000), false);
    var player = player(helper);
    var result = fill(helper, player, new ItemStack(Ic2Items.WATER_CELL, 3));
    helper.assertValueEqual(result, InteractionResult.FAIL, "full tank rejects");
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 10000, "tank remains full");
    helper.assertValueEqual(count(player, Ic2Items.WATER_CELL), 3, "all cells preserved");
    helper.assertValueEqual(count(player, Ic2Items.EMPTY_CELL), 0, "no empty cell created");
    noSpill(helper);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void lessThanOneBucketSpaceRollsBackTankAndInventory(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    for (Item item : List.of(Items.WATER_BUCKET, Ic2Items.WATER_CELL)) {
      player.getInventory().clearContent();
      boiler.waterTank.drainMbUnchecked(10000, false);
      boiler.waterTank.fillMb(Ic2FluidStack.create(Fluids.WATER, 9500), false);
      var result = fill(helper, player, new ItemStack(item));
      helper.assertValueEqual(result, InteractionResult.FAIL, "partial container rejected");
      helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 9500, "partial tank insert rolled back");
      helper.assertValueEqual(count(player, item), 1, "full container retained");
      helper.assertValueEqual(count(player, Items.BUCKET), 0, "no empty bucket");
      helper.assertValueEqual(count(player, Ic2Items.EMPTY_CELL), 0, "no empty cell");
      noSpill(helper);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void differentWaterTypesCannotMix(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    for (Fluid initial : List.of(Fluids.WATER, Ic2Fluids.DISTILLED_WATER.still())) {
      player.getInventory().clearContent();
      boiler.waterTank.drainMbUnchecked(10000, false);
      boiler.waterTank.fillMb(Ic2FluidStack.create(initial, 1000), false);
      Item item = initial == Fluids.WATER ? Ic2Items.DISTILLED_WATER_CELL : Ic2Items.WATER_CELL;
      helper.assertValueEqual(fill(helper, player, new ItemStack(item)), InteractionResult.FAIL,
          "mixing water types rejected");
      helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "original amount retained");
      helper.assertTrue(boiler.waterTank.getFluidStack().getFluid() == initial, "original type retained");
      helper.assertValueEqual(count(player, item), 1, "filled cell retained");
      noSpill(helper);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void lavaCannotFillOrSpillBesideBoiler(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    helper.assertValueEqual(fill(helper, player, new ItemStack(Items.LAVA_BUCKET)), InteractionResult.FAIL,
        "lava rejected");
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 0, "boiler stays empty");
    helper.assertValueEqual(count(player, Items.LAVA_BUCKET), 1, "lava bucket retained");
    noSpill(helper);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 60)
  public static void fullInventoryReturnsEmptyCellAsDrop(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    fill(helper, player, new ItemStack(Ic2Items.WATER_CELL, 2));
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "water transferred");
    helper.assertValueEqual(count(player, Ic2Items.WATER_CELL), 1, "one filled cell remains");
    helper.succeedWhen(() -> {
      int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
          new AABB(player.blockPosition()).inflate(5), e -> e.getItem().is(Ic2Items.EMPTY_CELL))
          .stream().mapToInt(e -> e.getItem().getCount()).sum();
      helper.assertValueEqual(dropped + count(player, Ic2Items.EMPTY_CELL), 1, "one returned empty cell");
    });
  }

  @GameTest(template = EMPTY)
  public static void creativeFillPreservesContainerWithoutEmptyCopies(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    player.setGameMode(GameType.CREATIVE);
    fill(helper, player, new ItemStack(Ic2Items.WATER_CELL, 3));
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "creative water transfer");
    helper.assertValueEqual(count(player, Ic2Items.WATER_CELL), 3, "creative cells preserved");
    helper.assertValueEqual(count(player, Ic2Items.EMPTY_CELL), 0, "no empty copies generated");
    noSpill(helper);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void spectatorAndAdventureCannotFill(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    for (GameType type : List.of(GameType.SPECTATOR, GameType.ADVENTURE)) {
      player.setGameMode(type);
      fill(helper, player, new ItemStack(Items.WATER_BUCKET));
      helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 0, "no transfer in " + type);
      helper.assertValueEqual(count(player, Items.WATER_BUCKET), 1, "bucket preserved in " + type);
      noSpill(helper);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void ordinaryClickStillOpensBoilerMenu(GameTestHelper helper) {
    var boiler = boiler(helper);
    var player = player(helper);
    click(helper, player, InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET), Direction.NORTH, false);
    helper.assertTrue(player.containerMenu instanceof ContainerSteamGenerator, "normal menu opens");
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 0, "normal click does not transfer");
    helper.assertValueEqual(count(player, Items.WATER_BUCKET), 1, "bucket retained");
    noSpill(helper);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void ordinaryBlocksStillAllowWorldWaterPlacement(GameTestHelper helper) {
    helper.setBlock(POS, Blocks.STONE);
    var player = player(helper);
    fill(helper, player, new ItemStack(Items.WATER_BUCKET));
    helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(POS.north())).isSource(),
        "ordinary world placement retained");
    helper.assertValueEqual(count(player, Items.BUCKET), 1, "world placement returns bucket");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nonTargetMachinesKeepTheirExistingInteraction(GameTestHelper helper) {
    helper.setBlock(POS, Ic2Blocks.MACERATOR);
    var player = player(helper);
    fill(helper, player, new ItemStack(Items.WATER_BUCKET));
    helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(POS.north())).isSource(),
        "non-target machine uses original world-placement path");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void emptyBucketsDoNotDrainInputTank(GameTestHelper helper) {
    var boiler = boiler(helper);
    boiler.waterTank.fillMb(Ic2FluidStack.create(Fluids.WATER, 1000), false);
    var player = player(helper);
    fill(helper, player, new ItemStack(Items.BUCKET));
    helper.assertValueEqual(boiler.waterTank.getFluidAmount(), 1000, "input tank not drained");
    helper.assertValueEqual(count(player, Items.BUCKET), 1, "empty bucket unchanged");
    noSpill(helper);
    helper.succeed();
  }
}
