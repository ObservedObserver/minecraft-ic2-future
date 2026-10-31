package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;

/** Shared clearance and trunk placement; TF subclasses retain their cap shapes. */
public abstract class MushroomFeatureBase extends ConfiguredFeatureBase<HugeMushroomFeatureConfiguration> {
    protected MushroomFeatureBase(Codec<HugeMushroomFeatureConfiguration> codec) { super(codec); }

    @Override
    public boolean place(FeaturePlacementContext<HugeMushroomFeatureConfiguration> context) {
        var level = context.level();
        var origin = context.origin();
        var random = context.random();
        var config = context.config();
        int height = getTreeHeight(random);
        var cursor = new BlockPos.MutableBlockPos();
        if (origin.getY() < level.getMinY() + 1 || origin.getY() + height + 1 > level.getMaxY()
                || !config.canPlaceOn().test(level, origin.below())) return false;
        for (int y = 0; y <= height; y++) {
            int radius = getTreeRadiusForHeight(-1, -1, config.foliageRadius(), y);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    var state = level.getBlockState(cursor.setWithOffset(origin, x, y, z));
                    if (!state.isAir() && !state.is(BlockTags.LEAVES)) return false;
                }
            }
        }
        makeCap(level, random, origin, height, cursor, config);
        placeTrunk(level, random, origin, config, height, cursor);
        return true;
    }

    protected int getTreeHeight(RandomSource random) {
        int height = random.nextInt(3) + 4;
        return random.nextInt(12) == 0 ? height * 2 : height;
    }

    protected int getTreeRadiusForHeight(int trunkHeight, int treeHeight, int foliageRadius, int y) {
        return y <= 3 ? 0 : foliageRadius;
    }

    protected void placeTrunk(WorldGenLevel level, RandomSource random, BlockPos origin,
                              HugeMushroomFeatureConfiguration config, int height, BlockPos.MutableBlockPos cursor) {
        for (int y = 0; y < height; y++) {
            cursor.set(origin).move(Direction.UP, y);
            // Evaluate the provider before the replacement check, as in 26.1.2.
            // Providers may consume randomness even when this block is retained.
            var newState = config.stemProvider().getState(level, random, origin);
            var current = level.getBlockState(cursor);
            if (current.isAir() || current.is(BlockTags.REPLACEABLE_BY_MUSHROOMS)) {
                setBlock(level, cursor, newState);
            }
        }
    }

    protected abstract void makeCap(WorldGenLevel level, RandomSource random, BlockPos origin, int height,
                                     BlockPos.MutableBlockPos cursor, HugeMushroomFeatureConfiguration config);
}
