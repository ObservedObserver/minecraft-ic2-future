package ic2.core.gametest;

import ic2.api.entity.block.ExplosiveEntity;
import ic2.compat.GameTest;
import ic2.core.entity.block.NukeEntity;
import ic2.core.ref.Ic2Entities;
import ic2.core.ref.Ic2Items;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public final class NativeEntityHookGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos TARGET = new BlockPos(1, 1, 1);
  private static final double EPSILON = 1.0E-5;

  private NativeEntityHookGameTests() {}

  @GameTest(template = EMPTY)
  public static void nativeInteractOnDisarmsNukeWithWrench(GameTestHelper helper) {
    ServerPlayer player = helper.makeMockServerPlayerInLevel();
    player.setGameMode(GameType.SURVIVAL);
    ItemStack wrench = new ItemStack(Ic2Items.WRENCH);
    player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
    NukeEntity nuke = spawnExplosive(helper, Ic2Entities.NUKE);
    try {
      InteractionResult result =
          player.interactOn(nuke, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.5, 0.0));

      helper.assertTrue(result.consumesAction(), "native interaction must consume the action");
      helper.assertTrue(nuke.isRemoved(), "wrench must disarm the nuke through Player.interactOn");
      helper.assertValueEqual(wrench.getDamageValue(), 1, "disarming must consume one durability");
    } finally {
      nuke.discard();
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeInteractOnLeavesNukeArmedWithEmptyHand(GameTestHelper helper) {
    ServerPlayer player = helper.makeMockServerPlayerInLevel();
    player.setGameMode(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    NukeEntity nuke = spawnExplosive(helper, Ic2Entities.NUKE);
    try {
      InteractionResult result =
          player.interactOn(nuke, InteractionHand.MAIN_HAND, new Vec3(0.0, 0.5, 0.0));

      helper.assertTrue(!result.consumesAction(), "empty hand must not disarm the nuke");
      helper.assertTrue(!nuke.isRemoved(), "nuke must remain armed after empty-hand interaction");
      helper.assertValueEqual(nuke.getFuse(), 200, "interaction must not change the fuse");
    } finally {
      nuke.discard();
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeMovementCollidesWithItntUntilDiscarded(GameTestHelper helper) {
    assertMovementCollision(helper, Ic2Entities.ITNT);
  }

  @GameTest(template = EMPTY)
  public static void nativeMovementCollidesWithNukeUntilDiscarded(GameTestHelper helper) {
    assertMovementCollision(helper, Ic2Entities.NUKE);
  }

  private static <T extends ExplosiveEntity> void assertMovementCollision(
      GameTestHelper helper, EntityType<T> type) {
    T explosive = spawnExplosive(helper, type);
    Cow mover = helper.spawn(EntityTypes.COW, TARGET);
    mover.setNoAi(true);
    mover.setNoGravity(true);
    Vec3 start = explosive.position().add(-1.0, 0.0, 0.0);
    Vec3 movement = new Vec3(2.0, 0.0, 0.0);
    try {
      mover.setPos(start);
      mover.move(MoverType.SELF, movement);
      helper.assertTrue(
          mover.getX() > start.x + EPSILON, "living entity must move toward the explosive");
      helper.assertTrue(
          mover.getBoundingBox().maxX <= explosive.getBoundingBox().minX + EPSILON,
          "native movement must stop at the explosive collision box");

      explosive.discard();
      mover.setPos(start);
      mover.move(MoverType.SELF, movement);
      helper.assertTrue(
          Math.abs(mover.getX() - (start.x + movement.x)) < EPSILON,
          "native movement must pass through the discarded explosive position");
    } finally {
      explosive.discard();
      mover.discard();
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeEyeHeightSurvivesDimensionRefresh(GameTestHelper helper) {
    ExplosiveEntity itnt = spawnExplosive(helper, Ic2Entities.ITNT);
    ExplosiveEntity nuke = spawnExplosive(helper, Ic2Entities.NUKE);
    try {
      assertEyeHeight(helper, itnt);
      assertEyeHeight(helper, nuke);
      itnt.refreshDimensions();
      nuke.refreshDimensions();
      assertEyeHeight(helper, itnt);
      assertEyeHeight(helper, nuke);
    } finally {
      itnt.discard();
      nuke.discard();
    }
    helper.succeed();
  }

  private static void assertEyeHeight(GameTestHelper helper, ExplosiveEntity explosive) {
    helper.assertTrue(
        Math.abs(explosive.getEyeHeight() - 0.15F) < EPSILON,
        "cached eye height must retain the explosive's 0.15-block offset");
    helper.assertTrue(
        Math.abs(explosive.getEyeHeight(Pose.STANDING) - 0.15F) < EPSILON,
        "pose eye height must retain the explosive's 0.15-block offset");
    helper.assertTrue(
        Math.abs(explosive.getEyeY() - explosive.getY() - 0.15F) < EPSILON,
        "world eye coordinate must use the same offset");
  }

  private static <T extends ExplosiveEntity> T spawnExplosive(
      GameTestHelper helper, EntityType<T> type) {
    T explosive = helper.spawn(type, TARGET);
    explosive.setFuse(200);
    explosive.setNoGravity(true);
    explosive.setDeltaMovement(Vec3.ZERO);
    return explosive;
  }
}
