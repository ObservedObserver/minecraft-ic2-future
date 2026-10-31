package twilightforest.datagen.data.loot;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import twilightforest.init.TFBlocks;
import twilightforest.init.TFItems;
import twilightforest.loot.TFLootTables;

import java.util.function.BiConsumer;

public record SpecialLootTables(LootTableSubProvider.Context registries) implements LootTableSubProvider {

	@Override
	public void run() {
        var consumer = this.registries;
		consumer.accept(TFLootTables.CICADA_SQUISH_DROPS, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(Items.DYE.pick(net.minecraft.world.item.DyeColor.GRAY)))));
		consumer.accept(TFLootTables.FIREFLY_SQUISH_DROPS, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(Items.GLOWSTONE_DUST))));
		consumer.accept(TFLootTables.MOONWORM_SQUISH_DROPS, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(Items.DYE.pick(net.minecraft.world.item.DyeColor.LIME)))));

		consumer.accept(TFLootTables.MOONWORM_FAILED_TO_PLACE_DROPS, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(TFBlocks.MOONWORM.asItem()))));

		consumer.accept(TFLootTables.LIFEDRAIN_SCEPTER_KILL_BONUS, LootTable.lootTable().withPool(
			LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1))
				.add(LootItem.lootTableItem(Items.ROTTEN_FLESH)).apply(SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.between(0, 2)))
		));

		consumer.accept(TFLootTables.KNIGHT_PHANTOM_DEFEATED, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(TFItems.KNIGHT_PHANTOM_TROPHY))));

		consumer.accept(TFLootTables.OMINOUS_SPAWNER_DROPS, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.exactly(1)).add(LootItem.lootTableItem(TFItems.EXANIMATE_ESSENCE))));
	}
}
