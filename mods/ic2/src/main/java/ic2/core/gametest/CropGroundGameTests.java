package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.crop.Ic2Crops;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.Ic2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/** Regression coverage for crop trampling, solid soil, and entities standing on a one-block farm. */
public final class CropGroundGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos CROP = new BlockPos(1, 2, 1);

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void sprintingContactDoesNotDestroyCropOrFarmland(GameTestHelper h) {
    TileEntityCrop crop = plant(h);
    Player player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setSprinting(true);
    for (int i = 0; i < 10000; i++) crop.onEntityCollision(player);
    h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
    h.assertBlockPresent(Ic2Blocks.WHEAT_CROP, CROP);
    h.assertTrue(h.getBlockEntity(CROP, TileEntityCrop.class).getCrop() == Ic2Crops.cropWheat,
        "sprinting must not reset crop card or stats");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground", timeoutTicks = 100)
  public static void fallingItemDoesNotDestroyCropOrSoil(GameTestHelper h) {
    TileEntityCrop crop = plant(h);
    var item = h.spawnItem(Items.COBBLESTONE, new Vec3(1.5, 4, 1.5));
    for (int i = 0; i < 1000; i++) crop.onEntityCollision(item);
    var soil = h.getBlockState(CROP.below());
    soil.getBlock().fallOn(h.getLevel(), soil, h.absolutePos(CROP.below()), item, 20);
    h.runAtTickTime(60, () -> {
      h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
      h.assertBlockPresent(Ic2Blocks.WHEAT_CROP, CROP);
      h.assertTrue(item.getY() >= h.absolutePos(CROP.below()).getY() + .9374,
          "falling item must land above the farmland surface");
      h.succeed();
    });
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void nativeFallCannotTrampleEmptyCropSticks(GameTestHelper h) {
    rack(h, CROP);
    fallOnSoil(h, h.makeMockPlayer(GameType.SURVIVAL));
    h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
    h.assertBlockPresent(Ic2Blocks.CROP_STICK, CROP);
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void nativeFallCannotTrampleCrossingSticks(GameTestHelper h) {
    rack(h, CROP).setCrossingBase(true);
    fallOnSoil(h, h.makeMockPlayer(GameType.SURVIVAL));
    h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
    h.assertTrue(h.getBlockEntity(CROP, TileEntityCrop.class).isCrossingBase(), "crossing base retained");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void nativeFallCannotTramplePlantedCrop(GameTestHelper h) {
    plant(h);
    fallOnSoil(h, h.makeMockPlayer(GameType.SURVIVAL));
    h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
    h.assertBlockPresent(Ic2Blocks.WHEAT_CROP, CROP);
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void ordinaryFarmlandCanStillBeTrampled(GameTestHelper h) {
    h.setBlock(CROP, Blocks.AIR);
    h.setBlock(CROP.below(), wetFarmland());
    fallOnSoil(h, h.makeMockPlayer(GameType.SURVIVAL));
    h.assertBlockPresent(Blocks.DIRT, CROP.below());
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void vanillaWheatDoesNotReceiveIc2TrampleProtection(GameTestHelper h) {
    h.setBlock(CROP.below(), wetFarmland());
    h.setBlock(CROP, Blocks.WHEAT);
    fallOnSoil(h, h.makeMockPlayer(GameType.SURVIVAL));
    h.assertBlockPresent(Blocks.DIRT, CROP.below());
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void sprintingOverWaterNeverDropsPlayerThroughExistingSoil(GameTestHelper h) {
    var crop = plant(h);
    h.setBlock(CROP.below(2), Blocks.WATER);
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(GameType.SURVIVAL);
    player.setNoGravity(true);
    player.setSprinting(true);
    var p = h.absolutePos(CROP.below());
    player.setPos(p.getX() + .5, p.getY() + .9375, p.getZ() + .5);
    for (int i = 0; i < 10000; i++) crop.onEntityCollision(player);
    for (int i = 0; i < 30; i++) player.move(MoverType.SELF, new Vec3(0, -.08, 0));
    h.assertTrue(player.getY() >= p.getY() + .9374,
        "player must not pass below solid soil; y=" + player.getY() + ", soil=" + h.getBlockState(CROP.below()));
    h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
    h.assertBlockPresent(Ic2Blocks.WHEAT_CROP, CROP);
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void allCropBlocksMaintainFarmlandIncludingEmptyAndPaleOak(GameTestHelper h) {
    int count = 0;
    for (Block block : BuiltInRegistries.BLOCK) {
      if (block instanceof Ic2TileEntityBlock cropBlock && cropBlock.getCropType() != null) {
        h.assertTrue(block.defaultBlockState().is(BlockTags.MAINTAINS_FARMLAND),
            "maintains_farmland tag missing " + BuiltInRegistries.BLOCK.getKey(block));
        count++;
      }
    }
    h.assertTrue(count >= 68, "all registered crop blocks checked");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void dryFarmlandUnderEmptyRackDoesNotRevertToDirt(GameTestHelper h) {
    assertDryFarmlandSurvives(h, false);
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void dryFarmlandUnderPlantedCropDoesNotRevertToDirt(GameTestHelper h) {
    assertDryFarmlandSurvives(h, true);
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void bareDryFarmlandStillRevertsNormally(GameTestHelper h) {
    var pos = CROP.above(20);
    var state = wetFarmland().setValue(FarmlandBlock.MOISTURE, 0);
    h.setBlock(pos.below(), state);
    state.randomTick(h.getLevel(), h.absolutePos(pos.below()), RandomSource.create(1));
    h.assertBlockPresent(Blocks.DIRT, pos.below());
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void weedGrassSpreadKeepsDirtSolidAboveWater(GameTestHelper h) {
    grassSpread(h, Blocks.DIRT.defaultBlockState(), false, false);
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void weedGrassSpreadWorksOnExistingGrassBlock(GameTestHelper h) {
    grassSpread(h, Blocks.GRASS_BLOCK.defaultBlockState(), false, false);
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void weedSpreadRaisesPlayerBeforeFarmlandBecomesFullBlock(GameTestHelper h) {
    grassSpread(h, wetFarmland(), true, false);
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void weedSpreadDoesNotOverwriteCeiling(GameTestHelper h) {
    grassSpread(h, Blocks.DIRT.defaultBlockState(), false, true);
  }

  @GameTest(template = EMPTY, batch = "crop_ground")
  public static void venomiliaContactEffectStillPoisonsWithoutTrampling(GameTestHelper h) {
    var crop = rack(h, CROP).transformCropBlock(Ic2Crops.cropVenomilia, 4);
    var cow = h.spawn(EntityTypes.COW, CROP);
    cow.setNoAi(true);
    crop.onEntityCollision(cow);
    h.assertTrue(cow.hasEffect(MobEffects.POISON), "crop-specific poison contact remains active");
    h.assertValueEqual(crop.getCurrentAge(), 3, "venomilia consumed its contact stage");
    h.assertBlockPresent(Blocks.FARMLAND, CROP.below());
    h.succeed();
  }

  private static void grassSpread(GameTestHelper h, BlockState soil, boolean playerOnTop, boolean ceiling) {
    var weed = rack(h, CROP).transformCropBlock(Ic2Crops.weed, 1);
    BlockPos target = CROP.east();
    for (Direction side : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST})
      h.setBlock(CROP.relative(side), Blocks.STONE);
    h.setBlock(target.below(2), Blocks.WATER);
    h.setBlock(target.below(), soil);
    h.setBlock(target, Blocks.AIR);
    h.setBlock(target.above(), ceiling ? Blocks.STONE : Blocks.AIR);
    var ground = h.absolutePos(target.below());
    Player player = null;
    if (playerOnTop) {
      var serverPlayer = h.makeMockServerPlayerInLevel();
      serverPlayer.setGameMode(GameType.SURVIVAL);
      serverPlayer.setNoGravity(true);
      serverPlayer.setPos(ground.getX() + .5, ground.getY() + .9375, ground.getZ() + .5);
      player = serverPlayer;
    }
    for (int i = 0; i < 100; i++) weed.performWeedWork();
    h.assertBlockPresent(Blocks.GRASS_BLOCK, target.below());
    h.assertTrue(!h.getBlockState(target.below()).getCollisionShape(h.getLevel(), ground).isEmpty(),
        "weed expansion must preserve solid soil");
    if (ceiling) {
      h.assertBlockPresent(Blocks.STONE, target.above());
      h.assertBlockPresent(Blocks.SHORT_GRASS, target);
    } else {
      h.assertBlockPresent(Blocks.TALL_GRASS, target);
      h.assertBlockPresent(Blocks.TALL_GRASS, target.above());
      h.assertTrue(h.getBlockState(target).getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER,
          "lower tall-grass half");
      h.assertTrue(h.getBlockState(target.above()).getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER,
          "upper tall-grass half");
    }
    if (player != null) {
      h.assertTrue(player.getY() >= ground.getY() + .9999, "player lifted out of added soil volume");
      for (int i = 0; i < 30; i++) player.move(MoverType.SELF, new Vec3(0, -.08, 0));
      h.assertTrue(player.getY() >= ground.getY() + .9999, "player stays on solid converted soil");
    }
    h.succeed();
  }

  private static void assertDryFarmlandSurvives(GameTestHelper h, boolean planted) {
    var pos = CROP.above(20);
    var crop = rack(h, pos);
    if (planted) crop.transformCropBlock(Ic2Crops.cropWheat, 1);
    var soil = wetFarmland().setValue(FarmlandBlock.MOISTURE, 0);
    h.setBlock(pos.below(), soil);
    for (int i = 0; i < 10; i++) soil.randomTick(h.getLevel(), h.absolutePos(pos.below()), RandomSource.create(i));
    h.assertBlockPresent(Blocks.FARMLAND, pos.below());
    h.assertTrue(h.getBlockEntity(pos, TileEntityCrop.class) != null, "rack survives dry farmland");
    h.succeed();
  }

  private static void fallOnSoil(GameTestHelper h, Entity entity) {
    var soil = h.getBlockState(CROP.below());
    soil.getBlock().fallOn(h.getLevel(), soil, h.absolutePos(CROP.below()), entity, 3);
  }

  private static TileEntityCrop plant(GameTestHelper h) {
    return rack(h, CROP).transformCropBlock(Ic2Crops.cropWheat, 1);
  }

  private static TileEntityCrop rack(GameTestHelper h, BlockPos pos) {
    h.setBlock(pos.below(), wetFarmland());
    h.setBlock(pos, Ic2Blocks.CROP_STICK.defaultBlockState().setValue(Ic2TileEntityBlock.CROSSING_BASE, false));
    return h.getBlockEntity(pos, TileEntityCrop.class);
  }

  private static BlockState wetFarmland() {
    return Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7);
  }
}
