package ic2.forge;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Optional Twilight Forest rewards. No Twilight Forest classes are linked when it is absent. */
final class TwilightForestLoot {
  private record Reward(ResourceKey<LootTable> table, boolean chest) {}
  private static final Map<Identifier, Reward> REWARDS = rewards();

  private TwilightForestLoot() {}

  private static Map<Identifier, Reward> rewards() {
    Map<Identifier, Reward> rewards = new HashMap<>();
    add(rewards, "common", true,
        "chests/hut_junk",
        "chests/basement",
        "chests/foundation_basement",
        "chests/fallen_trunk_loot",
        "chests/well",
        "chests/camp_tent",
        "chests/casket_loot",
        "chests/tower_grave_lower");
    add(rewards, "early", true,
        "chests/tree_cache",
        "chests/fancy_well",
        "chests/hedge_maze",
        "chests/hedge_cloth",
        "chests/hill_1",
        "chests/tower_foyer",
        "chests/tower_grave_upper",
        "chests/graveyard");
    add(rewards, "middle", true,
        "chests/hill_2",
        "chests/tower_room",
        "chests/tower_library",
        "chests/tower_potion",
        "chests/tower_enchanting",
        "chests/labyrinth_dead_end",
        "chests/troll_garden");
    add(rewards, "high", true,
        "chests/hill_3",
        "chests/labyrinth_room",
        "chests/stronghold_cache",
        "chests/darktower_cache",
        "chests/aurora_cache");
    add(rewards, "advanced", true,
        "chests/stronghold_room",
        "chests/darktower_key",
        "chests/aurora_room");
    add(rewards, "vault", true,
        "chests/labyrinth_vault",
        "chests/troll_vault");
    add(rewards, "jackpot", true,
        "chests/labyrinth_vault_jackpot",
        "chests/troll_vault_with_lamp");
    add(rewards, "final_boss", true,
        "chests/darktower_boss");
    add(rewards, "high", false, "entities/naga");
    add(rewards, "lich", false, "entities/lich");
    add(rewards, "middle_boss", false, "entities/minoshroom");
    add(rewards, "jackpot", false, "entities/hydra");
    add(rewards, "jackpot", false, "gameplay/knight_phantom_defeated");
    add(rewards, "final_boss", false, "entities/ur_ghast");
    add(rewards, "middle_boss", false, "entities/alpha_yeti");
    add(rewards, "final_boss", false, "entities/snow_queen");
    return Map.copyOf(rewards);
  }

  private static void add(Map<Identifier, Reward> rewards, String tier, boolean chest, String... paths) {
    var table = ResourceKey.create(Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath("ic2", "chests/twilightforest/" + tier));
    for (String path : paths) {
      var id = Identifier.fromNamespaceAndPath("twilightforest", path);
      if (rewards.put(id, new Reward(table, chest)) != null) {
        throw new IllegalStateException("Duplicate Twilight Forest reward: " + id);
      }
    }
  }

  static ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext context) {
    Reward reward = REWARDS.get(context.getQueriedLootTableId());
    if (reward == null) return loot;
    var origin = context.getOptional(LootContextParams.ORIGIN);
    if (origin == null) return loot;
    // Tower jars use the same tables as chests, but only accept a single item type.
    // Only real loot containers receive chest rewards; do not replace a jar's contents.
    if (reward.chest() && !(context.getLevel().getBlockEntity(BlockPos.containing(origin))
        instanceof RandomizableContainerBlockEntity)) return loot;
    // NeoForge invokes modifiers once after root-table expansion. Child references therefore
    // do not grant another reward, including the vault jackpot and lamp variants.
    var params = new LootParams.Builder(context.getLevel())
        .withParameter(LootContextParams.ORIGIN, origin)
        .create(LootContextParamSets.CHEST);
    var table = context.getLevel().getServer().reloadableRegistries().getLootTable(reward.table());
    // Use the original seed stream, and no luck bonus. One roll per container or boss reward.
    loot.addAll(table.getRandomItems(params, context.getRandom()));
    return loot;
  }
}
