package twilightforest.world.components.feature;

import twilightforest.world.components.feature.runtime.ConfiguredFeatureBase;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import twilightforest.world.components.feature.runtime.FeaturePlacementContext;
import twilightforest.world.components.feature.runtime.BlockStateConfiguration;
import twilightforest.init.TFBlocks;

public class LampostFeature extends ConfiguredFeatureBase<BlockStateConfiguration> {

	private static final Rotation[] ROTATIONS = Rotation.values();
	//private final BlockState lamp;

	public LampostFeature(Codec<BlockStateConfiguration> configIn) {
		super(configIn);
	}

	@Override
	public boolean place(FeaturePlacementContext<BlockStateConfiguration> ctx) {
		WorldGenLevel world = ctx.level();
		BlockPos pos = ctx.origin();
		RandomSource rand = ctx.random();
		BlockStateConfiguration config = ctx.config();

		// we should start on a grass block
		if (world.getBlockState(pos.below()).getBlock() != Blocks.GRASS_BLOCK) {
			return false;
		}

		// generate a height
		int height = 1 + rand.nextInt(4);

		// is it air or replaceable above our grass block
		for (int dy = 0; dy <= height; dy++) {
			BlockState state = world.getBlockState(pos.above(dy));
			if (!state.isAir() && !state.canBeReplaced()) {
				return false;
			}
		}

		// generate lamp
		for (int dy = 0; dy < height; dy++) {
			world.setBlock(pos.above(dy), TFBlocks.CANOPY_FENCE.get().defaultBlockState(), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
		}
		world.setBlock(pos.above(height), config.state().rotate(ROTATIONS[rand.nextInt(ROTATIONS.length)]), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
		return true;
	}
}
