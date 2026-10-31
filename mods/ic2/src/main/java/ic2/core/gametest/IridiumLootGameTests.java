package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public final class IridiumLootGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final int SAMPLE_COUNT = 128;

  private IridiumLootGameTests() {}

  @GameTest(template = EMPTY)
  public static void vanillaDungeonLootCanGenerateIridium(GameTestHelper helper) {
    LootTable dungeonLoot =
        helper
            .getLevel()
            .getServer()
            .reloadableRegistries()
            .getLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
    LootParams params =
        new LootParams.Builder(helper.getLevel())
            .withParameter(
                LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
            .create(LootContextParamSets.CHEST);

    for (int i = 0; i < SAMPLE_COUNT; i++) {
      for (ItemStack stack : dungeonLoot.getRandomItems(params)) {
        if (stack.is(Ic2Items.IRIDIUM_ORE) || stack.is(Ic2Items.IRIDIUM_SHARD)) {
          helper.succeed();
          return;
        }
      }
    }

    helper.fail(
        "Vanilla simple dungeon loot generated no IC2 iridium in " + SAMPLE_COUNT + " samples");
  }

  private record ShardCase(String path, String nativePath, double chance, int min, int max, boolean added) {}

  private static final List<ShardCase> SHARDS = List.of(
      new ShardCase("chests/abandoned_mineshaft", "chests/abandoned_mineshaft", 0.4347649094239774, 4, 10, false),
      new ShardCase("chests/desert_pyramid", "chests/desert_pyramid", 0.3495541273095448, 2, 4, false),
      new ShardCase("chests/end_city_treasure", "chests/end_city_treasure", 0.3495541273095448, 2, 4, false),
      new ShardCase("chests/igloo_chest", "chests/igloo_chest", 0.3495541273095448, 2, 4, false),
      new ShardCase("chests/jungle_temple", "chests/jungle_temple", 0.3495541273095448, 2, 4, false),
      new ShardCase("chests/nether_bridge", "chests/nether_bridge", 0.4347649094239774, 4, 10, false),
      new ShardCase("chests/simple_dungeon", "chests/simple_dungeon", 0.56243896484375, 12, 28, false),
      new ShardCase("chests/stronghold_corridor", "chests/stronghold_corridor", 0.3420986726771851, 8, 28, false),
      new ShardCase("chests/stronghold_crossing", "chests/stronghold_crossing", 0.3420986726771851, 4, 10, false),
      new ShardCase("chests/stronghold_library", "chests/stronghold_library", 0.3420986726771851, 4, 10, false),
      new ShardCase("chests/village_toolsmith", "chests/village/village_toolsmith", 0.29221665908056416, 6, 14, false),
      new ShardCase("chests/ancient_city", "chests/ancient_city", 0.56243896484375, 12, 28, true),
      new ShardCase("chests/ancient_city_ice_box", "chests/ancient_city_ice_box", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/trial_chambers/reward", "chests/trial_chambers/reward", 0.3420986726771851, 8, 28, true),
      new ShardCase("chests/trial_chambers/reward_ominous", "chests/trial_chambers/reward_ominous", 0.56243896484375, 12, 28, true),
      new ShardCase("chests/trial_chambers/supply", "chests/trial_chambers/supply", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/trial_chambers/corridor", "chests/trial_chambers/corridor", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/trial_chambers/intersection", "chests/trial_chambers/intersection", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/trial_chambers/intersection_barrel", "chests/trial_chambers/intersection_barrel", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/trial_chambers/entrance", "chests/trial_chambers/entrance", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/bastion_treasure", "chests/bastion_treasure", 0.56243896484375, 12, 28, true),
      new ShardCase("chests/bastion_other", "chests/bastion_other", 0.4347649094239774, 4, 10, true),
      new ShardCase("chests/bastion_bridge", "chests/bastion_bridge", 0.4347649094239774, 4, 10, true),
      new ShardCase("chests/bastion_hoglin_stable", "chests/bastion_hoglin_stable", 0.4347649094239774, 4, 10, true),
      new ShardCase("chests/shipwreck_map", "chests/shipwreck_map", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/shipwreck_supply", "chests/shipwreck_supply", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/shipwreck_treasure", "chests/shipwreck_treasure", 0.3420986726771851, 8, 28, true),
      new ShardCase("chests/buried_treasure", "chests/buried_treasure", 0.3420986726771851, 8, 28, true),
      new ShardCase("chests/ruined_portal", "chests/ruined_portal", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/pillager_outpost", "chests/pillager_outpost", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/woodland_mansion", "chests/woodland_mansion", 0.3420986726771851, 8, 28, true),
      new ShardCase("chests/underwater_ruin_small", "chests/underwater_ruin_small", 0.3495541273095448, 2, 4, true),
      new ShardCase("chests/underwater_ruin_big", "chests/underwater_ruin_big", 0.3495541273095448, 2, 4, true));

  private static LootParams chestParams(GameTestHelper helper) {
    return new LootParams.Builder(helper.getLevel())
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
        .create(LootContextParamSets.CHEST);
  }

  private static LootTable table(GameTestHelper helper, String namespace, String path) {
    ResourceKey<LootTable> key = ResourceKey.create(
        Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(namespace, path));
    return helper.getLevel().getServer().reloadableRegistries().getLootTable(key);
  }

  private static int shardCount(GameTestHelper helper, List<ItemStack> loot, ShardCase expected) {
    int total = 0;
    for (ItemStack stack : loot) {
      if (stack.is(Ic2Items.IRIDIUM_SHARD)) {
        helper.assertTrue(stack.getCount() >= expected.min() && stack.getCount() <= expected.max(),
            expected.path() + " shard stack must be within doubled range "
                + expected.min() + ".." + expected.max() + ", got " + stack.getCount());
        total += stack.getCount();
      }
    }
    helper.assertTrue(total <= 3 * expected.max(), expected.path() + " must inject at most three rolls");
    return total;
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void existingTablesShardRatesAndCountsAreDoubled(GameTestHelper helper) {
    LootParams params = chestParams(helper);
    for (ShardCase expected : SHARDS) {
      if (expected.added()) continue;
      LootTable lootTable = table(helper, "ic2", expected.path());
      RandomSource random = RandomSource.create(0x1C208L + expected.path().hashCode());
      int hits = 0;
      final int samples = 16384;
      for (int i = 0; i < samples; i++) {
        if (shardCount(helper, lootTable.getRandomItems(params, random), expected) > 0) hits++;
      }
      double observed = (double) hits / samples;
      System.out.println("[IC2 shard regression] " + expected.path()
          + " hits=" + hits + "/" + samples + " expected=" + expected.chance());
      helper.assertTrue(Math.abs(observed - expected.chance()) < 0.04,
          expected.path() + " per-chest shard probability must double: expected "
              + expected.chance() + ", got " + observed);
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void supportedNativeChestTablesProduceShards(GameTestHelper helper) {
    LootParams params = chestParams(helper);
    for (ShardCase expected : SHARDS) {
      LootTable vanilla = table(helper, "minecraft", expected.nativePath());
      int hits = 0;
      for (int i = 0; i < 128; i++) {
        if (shardCount(helper, vanilla.getRandomItems(params), expected) > 0) hits++;
      }
      helper.assertTrue(hits > 0, "native " + expected.path() + " must include IC2 shard loot");
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void trialVaultRootLootHasSingleShardInjection(GameTestHelper helper) {
    LootParams params = new LootParams.Builder(helper.getLevel())
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
        .create(LootContextParamSets.VAULT);
    for (ShardCase expected : SHARDS) {
      if (!expected.path().equals("chests/trial_chambers/reward")
          && !expected.path().equals("chests/trial_chambers/reward_ominous")) continue;
      LootTable vanilla = table(helper, "minecraft", expected.nativePath());
      int hits = 0;
      final int samples = 8192;
      for (int i = 0; i < samples; i++) {
        if (shardCount(helper, vanilla.getRandomItems(params), expected) > 0) hits++;
      }
      double observed = (double) hits / samples;
      System.out.println("[IC2 shard regression] " + expected.path()
          + " hits=" + hits + "/" + samples + " expected=" + expected.chance());
      helper.assertTrue(Math.abs(observed - expected.chance()) < 0.04,
          "vault root must inject once, not once per nested reward table: "
              + expected.path() + ", expected " + expected.chance() + ", got " + observed);
    }
    for (String child : List.of("reward_common", "reward_rare", "reward_unique",
        "reward_ominous_common", "reward_ominous_rare", "reward_ominous_unique")) {
      for (int i = 0; i < 64; i++) {
        for (ItemStack stack : table(helper, "minecraft", "chests/trial_chambers/" + child)
            .getRandomItems(params)) {
          helper.assertFalse(stack.is(Ic2Items.IRIDIUM_SHARD),
              "nested reward table must not independently inject shards: " + child);
        }
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void unmappedChestsStayOutsideShardInjection(GameTestHelper helper) {
    LootParams params = chestParams(helper);
    for (String path : List.of("chests/spawn_bonus_chest", "chests/village/village_armorer",
        "chests/village/village_weaponsmith")) {
      for (int i = 0; i < 128; i++) {
        for (ItemStack stack : table(helper, "minecraft", path).getRandomItems(params)) {
          helper.assertFalse(stack.is(Ic2Items.IRIDIUM_SHARD),
              "unmapped loot must not receive shards: " + path);
        }
      }
    }
    helper.succeed();
  }
}
