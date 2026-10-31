package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Keeps TF's placement algorithms independent of configuration binding. Each
 * registered algorithm owns a distinct native codec, including variant factories.
 * Decoding produces a complete native Feature with its configuration attached.
 */
public abstract class ConfiguredFeatureBase<C> {
    private final MapCodec<Instance<C>> codec;

    protected ConfiguredFeatureBase(Codec<C> configurationCodec) {
        codec = configurationCodec.fieldOf("config")
                .xmap(this::configured, Instance::config);
    }

    public final MapCodec<Instance<C>> configuredCodec() { return codec; }

    public final Instance<C> configured(C config) { return new Instance<>(this, config); }

    public abstract boolean place(FeaturePlacementContext<C> context);

    protected void setBlock(LevelWriter level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state);
    }

    protected void safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state, Predicate<BlockState> replaceable) {
        if (replaceable.test(level.getBlockState(pos))) level.setBlock(pos, state, 2);
    }

    protected void markAboveForPostProcessing(WorldGenLevel level, BlockPos pos) {
        var cursor = pos.mutable();
        for (int i = 0; i < 2; i++) {
            cursor.move(Direction.UP);
            if (level.getBlockState(cursor).isAir()) return;
            level.getChunk(cursor).markPosForPostProcessing(cursor);
        }
    }

    public record Instance<C>(ConfiguredFeatureBase<C> feature, C config) implements Feature {
        @Override
        public MapCodec<? extends Feature> codec() { return feature.configuredCodec(); }

        @Override
        public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
            return feature.place(new FeaturePlacementContext<>(level, generator, random, origin, config));
        }

        @Override
        public Stream<Holder<Feature>> getSubFeatures() {
            return config instanceof FeatureConfiguration configuration
                    ? configuration.getSubFeatures() : Stream.empty();
        }
    }
}
