package twilightforest.datagen.data.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContextSource;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import twilightforest.loot.TFLootTables;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LootGenerator extends LootTableProvider {

	public LootGenerator() {
		super(TFLootTables.allBuiltin(), List.of(
			new LootTableProvider.SubProviderEntry(BlockLootTables::new, LootContextParamSets.BLOCK),
			new LootTableProvider.SubProviderEntry(BlockInteractLootTables::new, LootContextParamSets.BLOCK_INTERACT),
			new LootTableProvider.SubProviderEntry(ChestLootTables::new, LootContextParamSets.CHEST),
			new LootTableProvider.SubProviderEntry(EntityLootTables::new, LootContextParamSets.ENTITY),
			new LootTableProvider.SubProviderEntry(EntityInteractLootTables::new, LootContextParamSets.ENTITY_INTERACT),
			new LootTableProvider.SubProviderEntry(SpecialLootTables::new, LootContextParamSets.EMPTY)
		));
	}

}
