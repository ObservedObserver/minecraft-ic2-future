package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.ref.Ic2Items;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

/** Tests optional mappings without requiring Twilight Forest to be installed. */
public final class TwilightForestLootGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);
  private record Expected(String path, double chance, int min, int max) {}
  private static final List<Expected> CHESTS = List.of(
      new Expected("chests/hut_junk", 0.1, 1, 3),
      new Expected("chests/basement", 0.1, 1, 3),
      new Expected("chests/foundation_basement", 0.1, 1, 3),
      new Expected("chests/fallen_trunk_loot", 0.1, 1, 3),
      new Expected("chests/well", 0.1, 1, 3),
      new Expected("chests/camp_tent", 0.1, 1, 3),
      new Expected("chests/casket_loot", 0.1, 1, 3),
      new Expected("chests/tower_grave_lower", 0.1, 1, 3),
      new Expected("chests/tree_cache", 0.2, 2, 4),
      new Expected("chests/fancy_well", 0.2, 2, 4),
      new Expected("chests/hedge_maze", 0.2, 2, 4),
      new Expected("chests/hedge_cloth", 0.2, 2, 4),
      new Expected("chests/hill_1", 0.2, 2, 4),
      new Expected("chests/tower_foyer", 0.2, 2, 4),
      new Expected("chests/tower_grave_upper", 0.2, 2, 4),
      new Expected("chests/graveyard", 0.2, 2, 4),
      new Expected("chests/hill_2", 0.3, 3, 6),
      new Expected("chests/tower_room", 0.3, 3, 6),
      new Expected("chests/tower_library", 0.3, 3, 6),
      new Expected("chests/tower_potion", 0.3, 3, 6),
      new Expected("chests/tower_enchanting", 0.3, 3, 6),
      new Expected("chests/labyrinth_dead_end", 0.3, 3, 6),
      new Expected("chests/troll_garden", 0.3, 3, 6),
      new Expected("chests/hill_3", 0.4, 4, 8),
      new Expected("chests/labyrinth_room", 0.4, 4, 8),
      new Expected("chests/stronghold_cache", 0.4, 4, 8),
      new Expected("chests/darktower_cache", 0.4, 4, 8),
      new Expected("chests/aurora_cache", 0.4, 4, 8),
      new Expected("chests/stronghold_room", 0.5, 6, 12),
      new Expected("chests/darktower_key", 0.5, 6, 12),
      new Expected("chests/aurora_room", 0.5, 6, 12),
      new Expected("chests/labyrinth_vault", 0.65, 8, 16),
      new Expected("chests/troll_vault", 0.65, 8, 16),
      new Expected("chests/labyrinth_vault_jackpot", 0.8, 12, 24),
      new Expected("chests/troll_vault_with_lamp", 0.8, 12, 24),
      new Expected("chests/darktower_boss", 1.0, 16, 32));
  private static final List<Expected> BOSSES = List.of(
      new Expected("entities/naga", 0.4, 4, 8),
      new Expected("entities/lich", 0.6, 6, 12),
      new Expected("entities/minoshroom", 0.75, 8, 16),
      new Expected("entities/hydra", 0.8, 12, 24),
      new Expected("gameplay/knight_phantom_defeated", 0.8, 12, 24),
      new Expected("entities/ur_ghast", 1.0, 16, 32),
      new Expected("entities/alpha_yeti", 0.75, 8, 16),
      new Expected("entities/snow_queen", 1.0, 16, 32));

  private static final List<Expected> TIERS = List.of(
      new Expected("common", .1, 1, 3), new Expected("early", .2, 2, 4),
      new Expected("middle", .3, 3, 6), new Expected("high", .4, 4, 8),
      new Expected("advanced", .5, 6, 12), new Expected("vault", .65, 8, 16),
      new Expected("jackpot", .8, 12, 24), new Expected("final_boss", 1, 16, 32),
      new Expected("lich", .6, 6, 12), new Expected("middle_boss", .75, 8, 16));

  private static LootParams params(GameTestHelper helper, float luck) {
    return new LootParams.Builder(helper.getLevel())
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(POS)))
        .withLuck(luck).create(LootContextParamSets.CHEST);
  }

  private static LootTable tier(GameTestHelper helper, String name) {
    return helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
        Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("ic2", "chests/twilightforest/" + name)));
  }

  private static ObjectArrayList<ItemStack> inject(GameTestHelper helper, String path, long seed, float luck) {
    LootContext context = new LootContext.Builder(params(helper, luck))
        .withOptionalRandomSource(RandomSource.create(seed)).create(Optional.empty());
    var loot = new ObjectArrayList<ItemStack>();
    loot.add(new ItemStack(Items.DIAMOND, 7));
    return CommonHooks.modifyLoot(Identifier.fromNamespaceAndPath("twilightforest", path), loot, context);
  }

  private static int shards(GameTestHelper helper, List<ItemStack> loot, Expected e) {
    int total = 0;
    int stacks = 0;
    for (ItemStack stack : loot) if (stack.is(Ic2Items.IRIDIUM_SHARD)) {
      stacks++;
      total += stack.getCount();
      helper.assertTrue(stack.getCount() >= e.min() && stack.getCount() <= e.max(),
          e.path() + " quantity outside approved range: " + stack.getCount());
    }
    helper.assertTrue(stacks <= 1, e.path() + " must grant at most one shard stack before container splitting");
    return total;
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void approvedTierProbabilitiesAndCounts(GameTestHelper helper) {
    for (Expected e : TIERS) {
      var table = tier(helper, e.path());
      helper.assertFalse(table == LootTable.EMPTY, "missing tier " + e.path());
      var random = RandomSource.create(0x1C210L + e.path().hashCode());
      int hits = 0, total = 0, smallest = Integer.MAX_VALUE, largest = 0;
      for (int i = 0; i < 16384; i++) {
        int count = shards(helper, table.getRandomItems(params(helper, 0), random), e);
        if (count > 0) { hits++; smallest = Math.min(smallest, count); largest = Math.max(largest, count); }
        total += count;
      }
      helper.assertTrue(Math.abs(hits / 16384.0 - e.chance()) < .025, "tier chance " + e.path());
      helper.assertValueEqual(smallest, e.min(), "minimum reachable " + e.path());
      helper.assertValueEqual(largest, e.max(), "maximum reachable " + e.path());
      System.out.println("[IC2 TF tier] " + e.path() + " hits=" + hits + "/16384 shards=" + total);
    }
    helper.succeed();
  }

  private static void mappings(GameTestHelper helper, List<Expected> expected) {
    for (Expected e : expected) {
      int hits = 0;
      for (int i = 0; i < 2048; i++) {
        var loot = inject(helper, e.path(), 0x26310L + i * 31L + e.path().hashCode(), 0);
        helper.assertTrue(loot.getFirst().is(Items.DIAMOND) && loot.getFirst().getCount() == 7,
            "must preserve existing loot " + e.path());
        if (shards(helper, loot, e) > 0) hits++;
      }
      helper.assertTrue(Math.abs(hits / 2048.0 - e.chance()) < .05,
          "native modifier mapping " + e.path() + ": " + hits + "/2048");
    }
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void allThirtySixChestMappingsMatchApprovedPlan(GameTestHelper helper) {
    helper.setBlock(POS, Blocks.CHEST);
    mappings(helper, CHESTS);
    helper.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void allEightBossRewardMappingsMatchApprovedPlan(GameTestHelper helper) {
    mappings(helper, BOSSES);
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void excludedAndIndividualKnightTablesNeverGrantShards(GameTestHelper helper) {
    helper.setBlock(POS, Blocks.CHEST);
    for (String path : List.of("chests/useless", "chests/suspicious_stew", "chests/just_bones",
        "chests/tower_jars", "chests/camp_armor_rack", "chests/camp_drying_rack", "chests/camp_pot",
        "chests/quest_grove_dropper", "entities/knight_phantom", "entities/deer", "chests/unknown")) {
      for (int i = 0; i < 128; i++) {
        var loot = inject(helper, path, i + 1, 0);
        helper.assertValueEqual(loot.size(), 1, "excluded table must preserve only original loot " + path);
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void sharedTowerTablesRequireActualLootContainer(GameTestHelper helper) {
    helper.setBlock(POS, Blocks.STONE);
    for (Expected e : CHESTS) {
      for (int i = 0; i < 32; i++) {
        helper.assertValueEqual(inject(helper, e.path(), i + 1, 0).size(), 1,
            "non-container must not receive chest reward " + e.path());
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void rewardsAreSeededAndIgnoreLuck(GameTestHelper helper) {
    helper.setBlock(POS, Blocks.CHEST);
    for (Expected e : CHESTS) {
      for (int i = 1; i <= 32; i++) {
        int first = shards(helper, inject(helper, e.path(), i, 0), e);
        int same = shards(helper, inject(helper, e.path(), i, 100), e);
        helper.assertValueEqual(first, same, "seeded amount independent of luck " + e.path());
      }
    }
    helper.succeed();
  }

  @GameTest(template = EMPTY)
  public static void nativeContainerResolvesSeedOnceAndDoesNotRefill(GameTestHelper helper) {
    helper.setBlock(POS, Blocks.CHEST);
    var chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(POS));
    var key = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath("ic2", "chests/twilightforest/final_boss"));
    chest.setLootTable(key, 12345L);
    chest.unpackLootTable(null);
    int count = 0;
    for (int slot = 0; slot < chest.getContainerSize(); slot++) {
      if (chest.getItem(slot).is(Ic2Items.IRIDIUM_SHARD)) count += chest.getItem(slot).getCount();
    }
    helper.assertTrue(count >= 16 && count <= 32, "native container reward range");
    helper.assertTrue(chest.getLootTable() == null, "loot table cleared after first resolution");
    chest.clearContent();
    chest.unpackLootTable(null);
    helper.assertTrue(chest.isEmpty(), "reopening cannot refill consumed loot");
    helper.succeed();
  }
}
