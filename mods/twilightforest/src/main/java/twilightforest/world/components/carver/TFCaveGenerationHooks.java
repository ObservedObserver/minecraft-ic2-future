package twilightforest.world.components.carver;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import twilightforest.world.components.TFCavesCarver;

public final class TFCaveGenerationHooks {
    private TFCaveGenerationHooks() { }
    public static boolean carve(WorldCarver carver, WorldGenerationContext context, RandomSource random,
            ChunkPos target, ChunkPos source, CarverOutput output, ChunkAccess chunk, RandomState randomState,
            MaterialRule materialRule, NoiseChunk noiseChunk, BiomeManager biomeManager, NoiseBasedChunkGenerator generator) {
        if (carver instanceof TFCavesCarver tf) {
            if (!(output instanceof TFCarvingMask mask)) throw new IllegalStateException("TF carving mask transformer did not apply");
            var resolver = generator.getBiomeSource().createUncachedResolver(randomState);
            var biomes = biomeManager.withDifferentSource(resolver);
            return tf.carve(context, random, target, source,
                new TFCaveDirectOutput(tf, mask, chunk, randomState, context, noiseChunk, materialRule, biomes::getBiome));
        }
        return carver.carve(context, random, target, source, output);
    }
}
