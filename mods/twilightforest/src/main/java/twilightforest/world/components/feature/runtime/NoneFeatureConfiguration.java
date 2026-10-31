package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public final class NoneFeatureConfiguration implements FeatureConfiguration {
    public static final NoneFeatureConfiguration NONE = new NoneFeatureConfiguration();
    public static final Codec<NoneFeatureConfiguration> CODEC = MapCodec.unit(() -> NONE).codec();
    private NoneFeatureConfiguration() { }
}
