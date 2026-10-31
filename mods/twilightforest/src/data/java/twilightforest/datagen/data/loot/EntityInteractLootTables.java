package twilightforest.datagen.data.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import twilightforest.init.TFItems;
import twilightforest.loot.TFLootTables;

import java.util.function.BiConsumer;

public record EntityInteractLootTables(LootTableSubProvider.Context registries) implements LootTableSubProvider {

	@Override
	public void run() {
        var consumer = this.registries;
		consumer.accept(TFLootTables.RAVEN_INTERACTION, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(TFItems.RAVEN_FEATHER))));
	}
}
