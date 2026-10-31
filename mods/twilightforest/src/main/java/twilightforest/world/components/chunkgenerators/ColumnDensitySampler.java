package twilightforest.world.components.chunkgenerators;

import net.minecraft.world.level.levelgen.densityfunction.*;

/** Reuses a Y-independent column sample only within one volume invocation. */
public final class ColumnDensitySampler<C> implements DensitySampler {
    @FunctionalInterface public interface Column<C> { C sample(SamplerContext context, int x, int y, int z); }
    @FunctionalInterface public interface Point<C> { float sample(C column, SamplerContext context, int x, int y, int z); }
    private final boolean yIndependent;
    private final Column<C> column;
    private final Point<C> point;
    public ColumnDensitySampler(boolean yIndependent, Column<C> column, Point<C> point) {
        this.yIndependent = yIndependent; this.column = column; this.point = point;
    }
    @Override public float sampleValue(SamplerContext context, int x, int y, int z) {
        return point.sample(column.sample(context, x, y, z), context, x, y, z);
    }
    @Override public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
        int index = 0;
        for (int z = 0; z < volume.sizeZ(); z++) {
            int blockZ = volume.blockZ(z);
            for (int x = 0; x < volume.sizeX(); x++) {
                int blockX = volume.blockX(x);
                C data = yIndependent && volume.sizeY() > 0 ? column.sample(context, blockX, volume.blockY(0), blockZ) : null;
                for (int y = 0; y < volume.sizeY(); y++) {
                    int blockY = volume.blockY(y);
                    C value = yIndependent ? data : column.sample(context, blockX, blockY, blockZ);
                    buffer.set(index++, point.sample(value, context, blockX, blockY, blockZ));
                }
            }
        }
    }
}
