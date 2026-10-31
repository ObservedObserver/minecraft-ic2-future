package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record HugeMushroomFeatureConfiguration(BlockStateProvider capProvider, BlockStateProvider stemProvider,
                                               int foliageRadius, BlockPredicate canPlaceOn) implements FeatureConfiguration {
    public static final Codec<HugeMushroomFeatureConfiguration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigurationCodecs.BLOCK_STATE_PROVIDER.fieldOf("cap_provider").forGetter(HugeMushroomFeatureConfiguration::capProvider),
            ConfigurationCodecs.BLOCK_STATE_PROVIDER.fieldOf("stem_provider").forGetter(HugeMushroomFeatureConfiguration::stemProvider),
            Codec.INT.optionalFieldOf("foliage_radius", 2).forGetter(HugeMushroomFeatureConfiguration::foliageRadius),
            BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(HugeMushroomFeatureConfiguration::canPlaceOn)
    ).apply(instance, HugeMushroomFeatureConfiguration::new));
}
