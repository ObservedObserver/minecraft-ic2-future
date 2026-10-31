package twilightforest.world.components.carver;

import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarverOutput;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import twilightforest.world.components.TFCavesCarver;

/** Applies TF's replacement policy at each original geometry visit, before the next tunnel is carved. */
public final class TFCaveDirectOutput implements CarverOutput {
    private static final LegacyDirtNoise DIRT_NOISE = new LegacyDirtNoise(new LegacyRandomSource(6972119253061020355L));
    private final TFCavesCarver carver;
    private final TFCaveConfiguration config;
    private final TFCarvingMask mask;
    private final ChunkAccess chunk;
    private final RandomState randomState;
    private final WorldGenerationContext context;
    private final NoiseChunk noiseChunk;
    private final MaterialRule materialRule;
    private final Function<BlockPos, Holder<Biome>> biomeGetter;
    private int columnX = -1;
    private int columnZ = -1;
    private boolean surface;

    public TFCaveDirectOutput(TFCavesCarver carver, TFCarvingMask mask, ChunkAccess chunk, RandomState randomState,
            WorldGenerationContext context, NoiseChunk noiseChunk, MaterialRule materialRule, Function<BlockPos, Holder<Biome>> biomeGetter) {
        this.carver = carver; this.config = carver.configuration(); this.mask = mask; this.chunk = chunk;
        this.randomState = randomState; this.context = context; this.noiseChunk = noiseChunk;
        this.materialRule = materialRule; this.biomeGetter = biomeGetter;
    }

    public void beginEllipsoid() { columnX = -1; columnZ = -1; surface = false; }
    @Override public int minY() { return mask.minY(); }
    @Override public int maxY() { return mask.maxY(); }

    @Override
    public void carve(int x, int y, int z) {
        if (columnX != x || columnZ != z) { columnX = x; columnZ = z; surface = false; }
        boolean firstVisit = mask.claim(x, y, z);
        if (!firstVisit && !config.debugSettings().debugMode()) return;
        BlockPos pos = new BlockPos(chunk.getPos().getBlockX(x), y, chunk.getPos().getBlockZ(z));
        BlockState before = chunk.getBlockState(pos);
        if (before.is(Blocks.GRASS_BLOCK) || before.is(Blocks.MYCELIUM) || before.is(Blocks.PODZOL) || before.is(Blocks.DIRT_PATH)) surface = true;
        if (y < chunk.getMinY() + 6) return;
        if (!canReplace(before) && !config.debugSettings().debugMode()) return;
        BlockPos origin = chunk.getPos().getWorldPosition();
        for (Direction direction : Direction.values()) {
            BlockPos relative = pos.relative(direction);
            if (inside(relative, origin) && chunk.getFluidState(relative).is(FluidTags.WATER)) return;
        }
        BlockState replacement = Blocks.CAVE_AIR.defaultBlockState();
        // This matches the former oreRandom() seed derivation, not a newly seeded stream.
        RandomSource random = randomState.getOrCreateRandomFactory(Identifier.withDefaultNamespace("ore")).at(pos);
        if (!chunk.getFluidState(pos.above(2)).isEmpty()) {
            replacement = random.nextBoolean() ? Blocks.ROOTED_DIRT.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
        }
        boolean placed = chunk.setBlockState(pos, replacement) != null;
        if (noiseChunk.aquifer().shouldScheduleFluidUpdate() && !replacement.getFluidState().isEmpty()) chunk.markPosForPostProcessing(pos);
        if (surface) {
            BlockPos below = pos.below();
            if (chunk.getBlockState(below).is(Blocks.DIRT)) {
                randomState.surfaceSystem().topMaterial(materialRule, randomState, context, biomeGetter, chunk,
                    noiseChunk.cachingSamplers(), below, !replacement.getFluidState().isEmpty()).ifPresent(state -> {
                        chunk.setBlockState(below, state);
                        if (!state.getFluidState().isEmpty()) chunk.markPosForPostProcessing(below);
                    });
            }
        }
        if (placed) decorateWalls(pos, random, origin);
    }

    private boolean canReplace(BlockState state) {
        return !state.is(BlockTags.ICE) && !state.getFluidState().is(FluidTags.WATER) && state.is(config.replaceable());
    }

    private void decorateWalls(BlockPos pos, RandomSource random, BlockPos origin) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!inside(adjacent, origin)) continue;
            if (carver.type().highlands) {
                if (random.nextInt(4) == 0 && canReplace(chunk.getBlockState(adjacent))) {
                    chunk.setBlockState(adjacent, carver.type().walls.getState(random, adjacent));
                }
            } else if (direction != Direction.DOWN && (direction == Direction.UP || chunk.getBlockState(adjacent.above()).isAir() || dirtAt(adjacent))) {
                BlockState neighbor = chunk.getBlockState(adjacent);
                if (neighbor.is(BlockTags.BASE_STONE_OVERWORLD) || neighbor.getFluidState().is(FluidTags.WATER)) {
                    chunk.setBlockState(adjacent, carver.type().walls.getState(random, adjacent));
                }
            }
        }
    }
    private static boolean inside(BlockPos pos, BlockPos origin) {
        int x = pos.getX() - origin.getX(); int z = pos.getZ() - origin.getZ();
        return x >= 0 && z >= 0 && x <= 15 && z <= 15;
    }
    private static boolean dirtAt(BlockPos pos) {
        return DIRT_NOISE.noise(pos.getX() * 0.25, pos.getY() * 0.25, pos.getZ() * 0.25) * 0.5 + 0.5 > 0.5;
    }
}
