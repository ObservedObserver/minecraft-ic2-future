package twilightforest.world.components.feature.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import twilightforest.world.components.feature.runtime.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record RootConfig(BlockStateProvider blockRoot, BlockStateProvider oreRoot) implements FeatureConfiguration {
	public static final Codec<RootConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		twilightforest.world.components.feature.runtime.ConfigurationCodecs.BLOCK_STATE_PROVIDER.fieldOf("root_block").forGetter(RootConfig::blockRoot),
		twilightforest.world.components.feature.runtime.ConfigurationCodecs.BLOCK_STATE_PROVIDER.fieldOf("root_ore").forGetter(RootConfig::oreRoot)
	).apply(instance, RootConfig::new));
}
