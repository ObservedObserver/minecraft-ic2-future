package ic2.forge;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class Ic2LootModifier extends LootModifier {
  public static final MapCodec<Ic2LootModifier> CODEC =
      RecordCodecBuilder.mapCodec(
          instance -> codecStart(instance).apply(instance, Ic2LootModifier::new));
  public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>>
      lootModifiersRegistry =
          DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "ic2");
  private static final Map<Identifier, net.minecraft.resources.ResourceKey<LootTable>>
      AllLootTables =
          Map.ofEntries(
              Map.entry(
                  BuiltInLootTables.ABANDONED_MINESHAFT.identifier(),
                  ic2Table("chests/abandoned_mineshaft")),
              Map.entry(
                  BuiltInLootTables.DESERT_PYRAMID.identifier(), ic2Table("chests/desert_pyramid")),
              Map.entry(
                  BuiltInLootTables.END_CITY_TREASURE.identifier(),
                  ic2Table("chests/end_city_treasure")),
              Map.entry(BuiltInLootTables.IGLOO_CHEST.identifier(), ic2Table("chests/igloo_chest")),
              Map.entry(
                  BuiltInLootTables.JUNGLE_TEMPLE.identifier(), ic2Table("chests/jungle_temple")),
              Map.entry(
                  BuiltInLootTables.NETHER_BRIDGE.identifier(), ic2Table("chests/nether_bridge")),
              Map.entry(
                  BuiltInLootTables.SIMPLE_DUNGEON.identifier(), ic2Table("chests/simple_dungeon")),
              Map.entry(
                  BuiltInLootTables.SPAWN_BONUS_CHEST.identifier(),
                  ic2Table("chests/spawn_bonus_chest")),
              Map.entry(
                  BuiltInLootTables.STRONGHOLD_CORRIDOR.identifier(),
                  ic2Table("chests/stronghold_corridor")),
              Map.entry(
                  BuiltInLootTables.STRONGHOLD_CROSSING.identifier(),
                  ic2Table("chests/stronghold_crossing")),
              Map.entry(
                  BuiltInLootTables.STRONGHOLD_LIBRARY.identifier(),
                  ic2Table("chests/stronghold_library")),
              Map.entry(
                  BuiltInLootTables.VILLAGE_TOOLSMITH.identifier(),
                  ic2Table("chests/village_toolsmith")),
              Map.entry(BuiltInLootTables.ANCIENT_CITY.identifier(),
                  ic2Table("chests/ancient_city")),
              Map.entry(BuiltInLootTables.ANCIENT_CITY_ICE_BOX.identifier(),
                  ic2Table("chests/ancient_city_ice_box")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD.identifier(),
                  ic2Table("chests/trial_chambers/reward")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_REWARD_OMINOUS.identifier(),
                  ic2Table("chests/trial_chambers/reward_ominous")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_SUPPLY.identifier(),
                  ic2Table("chests/trial_chambers/supply")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_CORRIDOR.identifier(),
                  ic2Table("chests/trial_chambers/corridor")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION.identifier(),
                  ic2Table("chests/trial_chambers/intersection")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_INTERSECTION_BARREL.identifier(),
                  ic2Table("chests/trial_chambers/intersection_barrel")),
              Map.entry(BuiltInLootTables.TRIAL_CHAMBERS_ENTRANCE.identifier(),
                  ic2Table("chests/trial_chambers/entrance")),
              Map.entry(BuiltInLootTables.BASTION_TREASURE.identifier(),
                  ic2Table("chests/bastion_treasure")),
              Map.entry(BuiltInLootTables.BASTION_OTHER.identifier(),
                  ic2Table("chests/bastion_other")),
              Map.entry(BuiltInLootTables.BASTION_BRIDGE.identifier(),
                  ic2Table("chests/bastion_bridge")),
              Map.entry(BuiltInLootTables.BASTION_HOGLIN_STABLE.identifier(),
                  ic2Table("chests/bastion_hoglin_stable")),
              Map.entry(BuiltInLootTables.SHIPWRECK_MAP.identifier(),
                  ic2Table("chests/shipwreck_map")),
              Map.entry(BuiltInLootTables.SHIPWRECK_SUPPLY.identifier(),
                  ic2Table("chests/shipwreck_supply")),
              Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE.identifier(),
                  ic2Table("chests/shipwreck_treasure")),
              Map.entry(BuiltInLootTables.BURIED_TREASURE.identifier(),
                  ic2Table("chests/buried_treasure")),
              Map.entry(BuiltInLootTables.RUINED_PORTAL.identifier(),
                  ic2Table("chests/ruined_portal")),
              Map.entry(BuiltInLootTables.PILLAGER_OUTPOST.identifier(),
                  ic2Table("chests/pillager_outpost")),
              Map.entry(BuiltInLootTables.WOODLAND_MANSION.identifier(),
                  ic2Table("chests/woodland_mansion")),
              Map.entry(BuiltInLootTables.UNDERWATER_RUIN_SMALL.identifier(),
                  ic2Table("chests/underwater_ruin_small")),
              Map.entry(BuiltInLootTables.UNDERWATER_RUIN_BIG.identifier(),
                  ic2Table("chests/underwater_ruin_big")));

  private static net.minecraft.resources.ResourceKey<LootTable> ic2Table(String path) {
    return net.minecraft.resources.ResourceKey.create(
        Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("ic2", path));
  }

  static {
    lootModifiersRegistry.register("inject", () -> CODEC);
  }

  public Ic2LootModifier(
      java.util.Optional<net.minecraft.core.Holder<LootItemCondition>> condition, int priority) {
    super(condition, priority);
  }

  @Override
  protected @NotNull ObjectArrayList<ItemStack> doApply(
      ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
    net.minecraft.resources.ResourceKey<LootTable> iLootId =
        AllLootTables.get(context.getQueriedLootTableId());
    if (iLootId == null) {
      return TwilightForestLoot.apply(generatedLoot, context);
    }
    LootTable table = context.getLevel().getServer().reloadableRegistries().getLootTable(iLootId);
    LootParams params =
        new LootParams.Builder(context.getLevel())
            .withParameter(LootContextParams.ORIGIN, context.getOptional(LootContextParams.ORIGIN))
            .withOptionalParameter(
                LootContextParams.THIS_ENTITY, context.getOptional(LootContextParams.THIS_ENTITY))
            .withLuck(context.getLuck())
            .create(LootContextParamSets.CHEST);
    generatedLoot.addAll(table.getRandomItems(params));
    return generatedLoot;
  }

  @Override
  public MapCodec<? extends IGlobalLootModifier> codec() {
    return CODEC;
  }
}
