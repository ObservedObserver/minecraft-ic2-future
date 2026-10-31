package ic2.core.gametest;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.prefab.BasicEnergyTe;
import ic2.compat.GameTest;
import ic2.core.block.comp.ComparatorEmitter;
import ic2.core.block.inherit.Ic2FenceBlock;
import ic2.core.block.personal.TileEntityPersonalChest;
import ic2.core.block.wiring.AbstractCableBlock;
import ic2.core.block.wiring.AbstractDetectorCableBlock;
import ic2.core.block.wiring.CableFoam;
import ic2.core.block.wiring.tileentity.TileEntityElectricMFSU;
import ic2.core.ref.Ic2BlockEntities;
import ic2.core.ref.Ic2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;

/** Regression tests must dispatch through the native API, not IC2's legacy overloads. */
public class NativeBlockHookGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void foamCableRejectsNativeBucketInterfaces(GameTestHelper helper) {
    for (Block block :
        new Block[] {
          Ic2Blocks.COPPER_FOAM_CABLE,
          Ic2Blocks.DETECTOR_FOAM_CABLE,
          Ic2Blocks.SPLITTER_FOAM_CABLE
        }) {
      for (CableFoam foam : CableFoam.values()) {
        BlockState state =
            block.defaultBlockState().setValue(AbstractCableBlock.foamProperty, foam);
        helper.setBlock(POS, state);
        helper.assertTrue(
            !((LiquidBlockContainer) block)
                .canPlaceLiquid(
                    null, helper.getLevel(), helper.absolutePos(POS), state, Fluids.WATER),
            "foam cable must reject native water placement: " + block + "/" + foam);
        helper.assertTrue(
            ((BucketPickup) block)
                .pickupBlock(null, helper.getLevel(), helper.absolutePos(POS), state)
                .isEmpty(),
            "native empty-bucket pickup must safely reject foam cable: " + block + "/" + foam);
        helper.assertTrue(
            helper.getBlockState(POS).equals(state), "bucket query must preserve foam");
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void ordinaryCableNativeBucketRoundTrip(GameTestHelper helper) {
    Block block = Ic2Blocks.COPPER_CABLE;
    helper.setBlock(POS, block);
    BlockPos absolute = helper.absolutePos(POS);
    LiquidBlockContainer container = (LiquidBlockContainer) block;
    helper.assertTrue(
        container.canPlaceLiquid(
            null, helper.getLevel(), absolute, helper.getBlockState(POS), Fluids.WATER),
        "dry cable must accept native water placement");
    helper.assertTrue(
        container.placeLiquid(
            helper.getLevel(), absolute, helper.getBlockState(POS), Fluids.WATER.getSource(false)),
        "water placement should succeed");
    helper.assertBlockProperty(POS, AbstractCableBlock.WATERLOGGED, true);
    helper.assertTrue(
        !container.canPlaceLiquid(
            null, helper.getLevel(), absolute, helper.getBlockState(POS), Fluids.WATER),
        "already-waterlogged cable must reject another bucket");
    ItemStack picked =
        ((BucketPickup) block)
            .pickupBlock(null, helper.getLevel(), absolute, helper.getBlockState(POS));
    helper.assertTrue(picked.is(Items.WATER_BUCKET), "native pickup must return a water bucket");
    helper.assertBlockProperty(POS, AbstractCableBlock.WATERLOGGED, false);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void machineComparatorReadsNativeDirectionalHook(GameTestHelper helper) {
    helper.setBlock(POS, Ic2Blocks.PERSONAL_CHEST);
    TileEntityPersonalChest chest =
        (TileEntityPersonalChest) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    ComparatorEmitter comparator = chest.getComponent(ComparatorEmitter.class);
    comparator.onWorldTick();
    assertAnalog(helper, 0);
    chest.contentSlot.put(0, new ItemStack(Items.STONE, 64));
    comparator.onWorldTick();
    assertAnalog(helper, 1);
    for (int i = 0; i < chest.contentSlot.size(); i++) {
      chest.contentSlot.put(i, new ItemStack(Items.STONE, 64));
    }
    comparator.onWorldTick();
    assertAnalog(helper, 15);
    helper.succeed();
  }

  private static void assertAnalog(GameTestHelper helper, int expected) {
    for (Direction direction : Direction.values()) {
      helper.assertValueEqual(
          helper
              .getBlockState(POS)
              .getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(POS), direction),
          expected,
          "native directional comparator output " + direction);
    }
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void detectorCableNativeAnalogOutputTracksCurrent(GameTestHelper helper) {
    helper.setBlock(POS.above(), Ic2Blocks.MFSU);
    helper.setBlock(POS, Ic2Blocks.DETECTOR_CABLE);
    helper.setBlock(POS.below(), Ic2Blocks.MFSU);
    assertAnalog(helper, 0);
    TileEntityElectricMFSU source =
        (TileEntityElectricMFSU)
            helper.getLevel().getBlockEntity(helper.absolutePos(POS.above()));
    // Detector analog output is floor(EU/t * 15 / 8192), so a 32 EU/t batbox reads 0.
    // 2048 EU/t gives 3 even after cable loss; this buffer outlasts the 200-tick timeout.
    source.energy.addEnergy(1000000.0);
    helper.succeedWhen(
        () -> {
          helper.assertBlockProperty(POS, AbstractDetectorCableBlock.active, true);
          double current =
              EnergyNet.instance
                  .getNodeStats(EnergyNet.instance.getTile(helper.getLevel(), helper.absolutePos(POS)))
                  .getEnergyIn();
          helper.assertTrue(
              current >= 2047.0,
              "MFSU fixture must sustain 2048 EU/t before cable loss, actual " + current);
          helper.assertValueEqual(
              helper
                  .getBlockState(POS)
                  .getAnalogOutputSignal(
                      helper.getLevel(), helper.absolutePos(POS), Direction.NORTH),
              3,
              "native detector analog output for 2048 EU/t, measured current " + current);
        });
  }

  @GameTest(template = EMPTY)
  public static void resinSheetDropsAfterNativeNeighborUpdate(GameTestHelper helper) {
    helper.setBlock(POS.below(), Blocks.STONE);
    helper.setBlock(POS, Ic2Blocks.RESIN_SHEET);
    helper
        .getLevel()
        .setBlockAndUpdate(helper.absolutePos(POS.below()), Blocks.AIR.defaultBlockState());
    helper.assertBlockPresent(Blocks.AIR, POS);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void resinSheetNativeContactSlowsEntity(GameTestHelper helper) {
    helper.setBlock(POS.below(), Blocks.STONE);
    helper.setBlock(POS, Ic2Blocks.RESIN_SHEET);
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.setDeltaMovement(1.0, -1.0, 1.0);
    player.fallDistance = 8.0;
    helper
        .getBlockState(POS)
        .entityInside(
            helper.getLevel(), helper.absolutePos(POS), player, InsideBlockEffectApplier.NOOP, true);
    Ic2GameTestAssertions.assertNear(
        helper, player.getDeltaMovement().x, 0.6, "resin horizontal slowdown");
    Ic2GameTestAssertions.assertNear(
        helper, player.getDeltaMovement().y, -0.85, "resin vertical slowdown");
    Ic2GameTestAssertions.assertNear(
        helper, player.fallDistance, 6.0, "resin fall-distance reduction");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void supportedRubberSheetNativeContactBounces(GameTestHelper helper) {
    helper.setBlock(POS.below(), Blocks.AIR);
    helper.setBlock(POS.west(), Blocks.STONE);
    helper.setBlock(POS.east(), Blocks.STONE);
    helper.setBlock(POS, Ic2Blocks.RUBBER_SHEET);
    Player player = helper.makeMockPlayer(GameType.SURVIVAL);
    player.setDeltaMovement(0.0, -1.0, 0.0);
    player.fallDistance = 8.0;
    helper
        .getBlockState(POS)
        .entityInside(
            helper.getLevel(), helper.absolutePos(POS), player, InsideBlockEffectApplier.NOOP, true);
    Ic2GameTestAssertions.assertNear(
        helper, player.getDeltaMovement().y, 0.8, "rubber-sheet rebound");
    Ic2GameTestAssertions.assertNear(helper, player.fallDistance, 0.0, "rubber-sheet fall reset");
    helper.assertBlockPresent(Ic2Blocks.RUBBER_SHEET, POS);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void fenceNativeShapeUpdateRechecksAllConnections(GameTestHelper helper) {
    helper.setBlock(POS.east(), Blocks.STONE);
    BlockState stale = Ic2Blocks.IRON_FENCE.defaultBlockState();
    BlockState updated =
        stale.updateShape(
            helper.getLevel(),
            helper.getLevel(),
            helper.absolutePos(POS),
            Direction.UP,
            helper.absolutePos(POS.above()),
            Blocks.AIR.defaultBlockState(),
            helper.getLevel().getRandom());
    helper.assertTrue(
        updated.getValue(Ic2FenceBlock.connectProperties.get(Direction.EAST)),
        "native shape update must use IC2's complete connection refresh");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void energyPrefabNativeSaveLoadPreservesEnergy(GameTestHelper helper) {
    // Detached prefabs borrow a valid registered type/state pair without replacing a world BE.
    BasicEnergyTe.Sink sink =
        new BasicEnergyTe.Sink(
            Ic2BlockEntities.MACERATOR,
            helper.absolutePos(POS),
            Ic2Blocks.MACERATOR.defaultBlockState(),
            1000,
            1);
    sink.setLevel(helper.getLevel());
    sink.getEnergyBuffer().setEnergyStored(123.5);
    CompoundTag saved = sink.saveCustomOnly(helper.getLevel().registryAccess());
    helper.assertTrue(saved.contains("IC2BasicSink"), "native save must include prefab energy NBT");
    sink.getEnergyBuffer().setEnergyStored(0.0);
    sink.loadWithComponents(
        TagValueInput.create(
            ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
    Ic2GameTestAssertions.assertNear(
        helper,
        sink.getEnergyBuffer().getEnergyStored(),
        123.5,
        "prefab sink native NBT round trip");

    BasicEnergyTe.Source source =
        new BasicEnergyTe.Source(
            Ic2BlockEntities.MACERATOR,
            helper.absolutePos(POS),
            Ic2Blocks.MACERATOR.defaultBlockState(),
            1000,
            1);
    source.setLevel(helper.getLevel());
    source.getEnergyBuffer().setEnergyStored(456.25);
    CompoundTag sourceSaved = source.saveCustomOnly(helper.getLevel().registryAccess());
    source.getEnergyBuffer().setEnergyStored(0.0);
    source.loadWithComponents(
        TagValueInput.create(
            ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), sourceSaved));
    Ic2GameTestAssertions.assertNear(
        helper,
        source.getEnergyBuffer().getEnergyStored(),
        456.25,
        "prefab source native NBT round trip");
    helper.succeed();
  }
}
