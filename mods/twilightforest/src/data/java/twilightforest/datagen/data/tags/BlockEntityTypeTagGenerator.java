package twilightforest.datagen.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import twilightforest.TwilightForestMod;
import twilightforest.init.TFBlockEntities;
import twilightforest.tags.TFBlockEntityTypeTags;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BlockEntityTypeTagGenerator extends TagsProvider<BlockEntityType<?>> {

	public BlockEntityTypeTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> provider) {
		super(output, Registries.BLOCK_ENTITY_TYPE, provider, TwilightForestMod.ID);
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
		this.tag(TFBlockEntityTypeTags.RELOCATION_NOT_SUPPORTED).addAll(List.of(
			TFBlockEntities.ANTIBUILDER.unwrapKey().orElseThrow(),
			TFBlockEntities.BEANSTALK_GROWER.unwrapKey().orElseThrow(),
			TFBlockEntities.NAGA_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.LICH_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.MINOSHROOM_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.HYDRA_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.KNIGHT_PHANTOM_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.UR_GHAST_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.ALPHA_YETI_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.SNOW_QUEEN_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.FINAL_BOSS_SPAWNER.unwrapKey().orElseThrow()));

		this.tag(TFBlockEntityTypeTags.IMMOVABLE).addAll(List.of(
			TFBlockEntities.ANTIBUILDER.unwrapKey().orElseThrow(),
			TFBlockEntities.BEANSTALK_GROWER.unwrapKey().orElseThrow(),
			TFBlockEntities.NAGA_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.LICH_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.MINOSHROOM_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.HYDRA_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.KNIGHT_PHANTOM_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.UR_GHAST_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.ALPHA_YETI_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.SNOW_QUEEN_SPAWNER.unwrapKey().orElseThrow(),
			TFBlockEntities.FINAL_BOSS_SPAWNER.unwrapKey().orElseThrow()));
	}

	@Override
	public String getName() {
		return "Twilight Forest Block Entity Tags";
	}
}
