package validation;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.levelgen.densityfunction.*;
import org.junit.jupiter.api.Test;
import twilightforest.world.components.chunkgenerators.ColumnDensitySampler;
import twilightforest.world.components.chunkgenerators.FocusedDensityFunction;
import static org.junit.jupiter.api.Assertions.*;

class ColumnDensitySamplerTest {
    @Test
    void yIndependentColumnIsSampledOnceButPointChildrenStillSeeEachY() {
        var calls = new AtomicInteger();
        var sampler = new ColumnDensitySampler<Integer>(true, (context, x, y, z) -> { calls.incrementAndGet(); return x + z; },
            (column, context, x, y, z) -> column + y);
        var volume = new DensityVolume(2, 3, 2, -16, -20, 32, 4, 2, 8);
        var buffer = DensityBuffer.createUnpooled(volume.size());
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, volume);
        assertEquals(4, calls.get());
        for (int z = 0; z < volume.sizeZ(); z++) for (int x = 0; x < volume.sizeX(); x++) for (int y = 0; y < volume.sizeY(); y++) {
            assertEquals(volume.blockX(x) + volume.blockY(y) + volume.blockZ(z), buffer.get(volume.indexUnchecked(x, y, z)));
        }
    }

    @Test
    void yDependentColumnKeepsVerticalVariation() {
        var calls = new AtomicInteger();
        var sampler = new ColumnDensitySampler<Integer>(false, (context, x, y, z) -> { calls.incrementAndGet(); return y * y; },
            (column, context, x, y, z) -> column);
        var volume = new DensityVolume(1, 3, 1, 0, -3, 0);
        var buffer = DensityBuffer.createUnpooled(volume.size());
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, volume);
        assertEquals(3, calls.get());
        assertEquals(9, buffer.get(0)); assertEquals(4, buffer.get(1)); assertEquals(1, buffer.get(2));
    }

    @Test
    void subsequentVolumesDoNotReuseAnotherChunkColumn() {
        var sampler = new ColumnDensitySampler<Integer>(true, (context, x, y, z) -> x - z,
            (column, context, x, y, z) -> column);
        var first = new DensityVolume(1, 2, 1, 0, 0, 0);
        var next = new DensityVolume(1, 2, 1, 16, 0, -16);
        var buffer = DensityBuffer.createUnpooled(2);
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, first);
        assertEquals(0, buffer.get(0));
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, next);
        assertEquals(32, buffer.get(0)); assertEquals(32, buffer.get(1));
    }

    @Test
    void sameCoordinatesUseTheCurrentSamplerContext() {
        ContextKey<Integer> key = new ContextKey<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("density_sqrt_validation", "column_test"));
        var first = SamplerContext.builder().setUserFields(ContextMap.builder().set(key, 3).build()).build();
        var next = SamplerContext.builder().setUserFields(ContextMap.builder().set(key, 11).build()).build();
        var sampler = new ColumnDensitySampler<Integer>(true, (context, x, y, z) -> context.getField(key),
            (column, context, x, y, z) -> column);
        assertEquals(3, sampler.sampleValue(first, 0, 0, 0));
        assertEquals(11, sampler.sampleValue(next, 0, 0, 0));
        var volume = new DensityVolume(1, 2, 1, 0, 0, 0);
        var buffer = DensityBuffer.createUnpooled(2);
        sampler.sampleVolume(first, buffer, volume);
        assertEquals(3, buffer.get(0));
        sampler.sampleVolume(next, buffer, volume);
        assertEquals(11, buffer.get(0));
    }

    @Test
    void focusedSphereDeclaresItsActualNegativeAndPositiveRange() {
        var function = new FocusedDensityFunction(0, 0, 0, 10, -12, 1);
        var sampler = function.compileSampler(null);
        assertEquals(-12, sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, 0, 0, 0));
        assertEquals(1, sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, 10, 0, 0));
        assertEquals(1, sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, 20, 0, 0));
        assertEquals(-12, function.range().min()); assertEquals(1, function.range().max());
    }
}
