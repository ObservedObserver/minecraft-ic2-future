package ic2.core.gametest;

import com.mojang.authlib.GameProfile;
import ic2.compat.GameTest;
import ic2.core.Ic2DamageSource;
import ic2.core.Ic2Potion;
import ic2.core.ref.Ic2Items;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Exercise Minecraft's effect dispatch, never the obsolete IC2 two-argument overload. */
public class NativeEffectHookGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static ServerPlayer player(GameTestHelper helper) {
    // The helper's in-level mock hardcodes CREATIVE and never acknowledges client loading.
    // Use the same native login setup with a normal player and complete the load handshake.
    UUID id = UUID.randomUUID();
    GameProfile profile =
        new GameProfile(id, "ic2_" + id.toString().replace("-", "").substring(0, 12));
    CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
    var server = helper.getLevel().getServer();
    ServerPlayer player =
        new ServerPlayer(server, helper.getLevel(), profile, cookie.clientInformation());
    Connection connection = new Connection(PacketFlow.SERVERBOUND);
    new EmbeddedChannel(connection);
    server.getPlayerList().placeNewPlayer(connection, player, cookie);
    player.setGameMode(GameType.SURVIVAL);
    player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
    helper.assertValueEqual(player.gameMode(), GameType.SURVIVAL, "fixture survival game mode");
    helper.assertFalse(player.getAbilities().invulnerable, "fixture must not have creative immunity");
    helper.assertTrue(player.connection.hasClientLoaded(), "fixture must finish client loading");
    player.setHealth(20.0F);
    return player;
  }

  private static void tickNativeEffect(GameTestHelper helper, ServerPlayer player) {
    MobEffectInstance effect = player.getEffect(Ic2Potion.radiationHolder());
    helper.assertTrue(effect != null, "radiation must be attached before native effect update");
    effect.tickServer(helper.getLevel(), player, () -> {});
  }

  @GameTest(template = EMPTY)
  public static void nativeEffectUpdateDamagesSurvivalPlayer(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    Ic2Potion.radiation.applyTo(player, 100, 0);
    tickNativeEffect(helper, player);
    Ic2GameTestAssertions.assertNear(helper, player.getHealth(), 19.5, "radiation level zero damage");
    helper.assertTrue(player.hasEffect(Ic2Potion.radiationHolder()), "damage must not consume effect");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeEffectUsesCurrentWorldAndAmplifier(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    var previousSource = Ic2DamageSource.radiation;
    try {
      Ic2DamageSource.radiation = null;
      Ic2Potion.radiation.applyTo(player, 100, 100);
      tickNativeEffect(helper, player);
      Ic2GameTestAssertions.assertNear(helper, player.getHealth(), 18.5, "radiation scaled damage");
      helper.assertTrue(Ic2DamageSource.radiation == null, "damage must not need global source cache");
    } finally {
      Ic2DamageSource.radiation = previousSource;
    }
    helper.succeed();
  }

  private static void equipHazmat(ServerPlayer player) {
    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Ic2Items.HAZMAT_HELMET));
    player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Ic2Items.HAZMAT_CHESTPLATE));
    player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Ic2Items.HAZMAT_LEGGINGS));
    player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Ic2Items.RUBBER_BOOTS));
  }

  @GameTest(template = EMPTY)
  public static void nativeEffectStillRespectsCompleteHazmat(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    equipHazmat(player);
    Ic2Potion.radiation.applyTo(player, 100, 100);
    tickNativeEffect(helper, player);
    Ic2GameTestAssertions.assertNear(helper, player.getHealth(), 20.0, "complete hazmat protection");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeInventoryExposureLeadsToRadiationDamage(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    player.getInventory().setItem(0, new ItemStack(Ic2Items.URANIUM));
    player.getInventory().tick();
    helper.assertTrue(player.hasEffect(Ic2Potion.radiationHolder()), "uranium inventory exposure");
    tickNativeEffect(helper, player);
    helper.assertTrue(player.getHealth() < 20.0F, "native inventory exposure must lead to damage");
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeInventoryHazmatPreventsExposure(GameTestHelper helper) {
    ServerPlayer player = player(helper);
    equipHazmat(player);
    player.getInventory().setItem(0, new ItemStack(Ic2Items.URANIUM));
    player.getInventory().tick();
    helper.assertFalse(player.hasEffect(Ic2Potion.radiationHolder()), "hazmat prevents new exposure");
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 70)
  public static void naturallyTickingMobTakesRadiationDamage(GameTestHelper helper) {
    Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(1, 1, 1));
    float before = pig.getHealth();
    Ic2Potion.radiation.applyTo(pig, 100, 0);
    helper.runAfterDelay(26, () -> {
      helper.assertTrue(pig.getHealth() < before, "natural living-entity effect update must damage");
      helper.succeed();
    });
  }
}
