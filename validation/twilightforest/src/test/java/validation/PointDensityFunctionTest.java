package validation;

import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import org.junit.jupiter.api.Test;
import twilightforest.world.components.chunkgenerators.AbsoluteDifferenceFunction;
import twilightforest.world.components.chunkgenerators.HollowHillFunction;
import twilightforest.world.components.chunkgenerators.PointDensityFunction;
import twilightforest.world.components.chunkgenerators.TanhHillFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PointDensityFunctionTest {
    private static float sample(PointDensityFunction function, int x, int y, int z) {
        return function.compileSampler(null).sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z);
    }

    @Test
    void minDistanceKeepsAxisAndCapBehavior() {
        var min = new AbsoluteDifferenceFunction.Min(16, 10, -4);
        assertEquals(0, sample(min, 10, 64, -4));
        assertEquals(0, sample(min, 20, 64, -4));
        assertEquals(10, sample(min, 20, -64, 6));
        assertEquals(16, sample(min, 100, 64, -100));
    }

    @Test
    void maxDistanceKeepsAxisAndCapBehavior() {
        var max = new AbsoluteDifferenceFunction.Max(16, 10, -4);
        assertEquals(0, sample(max, 10, 64, -4));
        assertEquals(10, sample(max, 20, 64, -4));
        assertEquals(10, sample(max, 20, -64, 6));
        assertEquals(16, sample(max, 100, 64, -100));
    }

    @Test
    void hollowHillPreservesCenterRoofAndRadiusBoundary() {
        var hill = new HollowHillFunction(0, 64, 0, 12, 1);
        assertEquals(1, sample(hill, 0, 64, 0));
        assertEquals(-1, sample(hill, 0, 69, 0));
        assertEquals(0, sample(hill, 12, 64, 0));
        assertEquals(0, sample(hill, -12, 64, 0));
    }

    @Test
    void negativeRadiusPreservesHollowHillFloor() {
        var floor = new HollowHillFunction(0, 64, 0, -12, 1);
        assertEquals(1, sample(floor, 0, 59, 0));
        assertEquals(0, sample(floor, 0, 60, 0));
        assertEquals(-1, sample(floor, 0, 61, 0));
    }

    @Test
    void trunkMoundPreservesSideAndVerticalRejection() {
        var mound = new TanhHillFunction(0, 64, 0, 10, 2, 0, true, true);
        assertEquals(1, sample(mound, 1, 64, 1));
        assertEquals(-1, sample(mound, 1, 64, -1));
        assertEquals(-1, sample(mound, 1, 75, 1));
        assertEquals(-1, sample(mound, 40, 64, 1));
    }

    @Test
    void volumeSamplingUsesGameCoordinateStepsAndYFirstBufferOrder() {
        var hill = new HollowHillFunction(0, 0, 0, 12, 1);
        var volume = new DensityVolume(2, 2, 2, 0, 3, 0, 12, 2, 12);
        var output = DensityBuffer.createUnpooled(volume.size());
        hill.compileSampler(null).sampleVolume(SamplerContext.EMPTY_UNCACHED, output, volume);
        var values = new float[volume.size()];
        for (int i = 0; i < values.length; i++) values[i] = output.get(i);
        assertArrayEquals(new float[]{1, -1, 0, 0, 0, 0, 0, 0}, values);
    }
}
