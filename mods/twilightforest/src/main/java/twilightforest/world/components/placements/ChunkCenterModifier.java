package twilightforest.world.components.placements;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.stream.Stream;

public class ChunkCenterModifier implements PlacementModifier {
	private static final ChunkCenterModifier INSTANCE = new ChunkCenterModifier();
	public static final MapCodec<ChunkCenterModifier> CODEC = MapCodec.unit(() -> INSTANCE);

	public static ChunkCenterModifier center() {
		return INSTANCE;
	}

	public Stream<BlockPos> getPositions(PlacementContext ctx, RandomSource random, BlockPos pos) {
		return Stream.of(new BlockPos((pos.getX() & 0xfffffff0) + 8, pos.getY(), (pos.getZ() & 0xfffffff0) + 8));
	}

	@Override
	public MapCodec<ChunkCenterModifier> codec() { return CODEC; }
    @Override
    public void modify(PlacementContext context, RandomSource random, BlockPos origin, java.util.function.Consumer<BlockPos> output) {
        // Keep the original stream construction before downstream placement. In
        // particular, blanketing rolls all candidate positions before applying them.
        getPositions(context, random, origin).forEach(output);
    }
}
