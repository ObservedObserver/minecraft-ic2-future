package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record DiskConfiguration(BlockStateProvider stateProvider, BlockPredicate target,
                                IntProvider radius, int halfHeight) implements FeatureConfiguration {
    public static final Codec<DiskConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigurationCodecs.BLOCK_STATE_PROVIDER.fieldOf("state_provider").forGetter(DiskConfiguration::stateProvider),
            BlockPredicate.CODEC.fieldOf("target").forGetter(DiskConfiguration::target),
            IntProviders.NON_NEGATIVE_CODEC.fieldOf("radius").forGetter(DiskConfiguration::radius),
            Codec.intRange(0, 16).fieldOf("half_height").forGetter(DiskConfiguration::halfHeight)
    ).apply(instance, DiskConfiguration::new));
}
