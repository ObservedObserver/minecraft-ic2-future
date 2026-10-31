package twilightforest.datagen.data;

import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import twilightforest.TwilightForestMod;
import twilightforest.init.TFItems;
import twilightforest.loot.conditions.GiantPickUsedCondition;
import twilightforest.loot.modifiers.FieryToolSmeltingModifier;
import twilightforest.loot.modifiers.GiantToolGroupingModifier;

import java.util.concurrent.CompletableFuture;

public class LootModifierGenerator extends GlobalLootModifierProvider {
	public LootModifierGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
		super(output, provider, TwilightForestMod.ID);
	}

	@Override
	protected void start() {
		add("fiery_pick_smelting", new FieryToolSmeltingModifier(java.util.Optional.of(net.minecraft.core.Holder.direct(MatchTool.toolMatches(ItemPredicate.Builder.item().of(registries.lookupOrThrow(Registries.ITEM), TFItems.FIERY_PICKAXE.get())).build())), IGlobalLootModifier.DEFAULT_PRIORITY));
		add("giant_pick_grouping", new GiantToolGroupingModifier(java.util.Optional.of(net.minecraft.core.Holder.direct(GiantPickUsedCondition.builder(LootContext.EntityTarget.THIS).build())), IGlobalLootModifier.DEFAULT_PRIORITY));
	}
}
