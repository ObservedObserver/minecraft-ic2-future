package twilightforest.world.components.feature.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

/** Parameters passed from the native 26.3 feature to a TF placement algorithm. */
public record FeaturePlacementContext<C>(WorldGenLevel level, ChunkGenerator chunkGenerator,
                                         RandomSource random, BlockPos origin, C config) {
}
