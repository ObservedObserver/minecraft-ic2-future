package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public final class ConfigurationCodecs {
    private ConfigurationCodecs() { }
    // TF algorithms consume resolved providers. Resolve references while the
    // enclosing feature configuration is decoded, using the native holder codec.
    public static final Codec<BlockStateProvider> BLOCK_STATE_PROVIDER = BlockStateProvider.CODEC
            .xmap(Holder::value, Holder::direct);
}
