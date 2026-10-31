package twilightforest.world.components.feature;

import twilightforest.world.components.feature.runtime.ConfiguredFeatureBase;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import twilightforest.world.components.feature.runtime.FeaturePlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import twilightforest.world.components.feature.config.WeightedListFeatureConfig;

import java.util.Optional;

/**
 * While seemingly similar to RandomFeatureConfiguration, this uses a weighted list and a single random value sample.
 * The goal is to produced pseudorandom result distribution that better match expectations defined by a weighted list.
 */
public class WeightedListFeature extends ConfiguredFeatureBase<WeightedListFeatureConfig> {
	public WeightedListFeature(Codec<WeightedListFeatureConfig> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlacementContext<WeightedListFeatureConfig> context) {
		WeightedListFeatureConfig weightedListFeatureConfig = context.config();
		RandomSource random = context.random();
		WorldGenLevel worldGenLevel = context.level();
		ChunkGenerator chunkGenerator = context.chunkGenerator();
		BlockPos blockPos = context.origin();

		Optional<Holder<PlacedFeature>> randomFeature = weightedListFeatureConfig.getRandomFeature(random);

		//noinspection OptionalIsPresent
		if (randomFeature.isEmpty())
			return false;

		return randomFeature.get().value().place(worldGenLevel, chunkGenerator, random, blockPos);
	}
}
