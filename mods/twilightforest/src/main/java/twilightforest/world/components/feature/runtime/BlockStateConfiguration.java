package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.block.state.BlockState;

public record BlockStateConfiguration(BlockState state) implements FeatureConfiguration {
    public static final Codec<BlockStateConfiguration> CODEC = BlockState.CODEC.fieldOf("state")
            .xmap(BlockStateConfiguration::new, BlockStateConfiguration::state).codec();
}
