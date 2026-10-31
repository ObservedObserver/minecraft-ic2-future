package twilightforest.world.components.chunkgenerators;

import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/** Adapts immutable, childless point formulas to the 26.3 volume sampler. */
public interface PointDensityFunction extends DensityFunction {
    double computeAt(int blockX, int blockY, int blockZ);

    double minValue();

    double maxValue();

    @Override
    default DensitySampler compileSampler(CompileContext context) {
        return new DensitySampler() {
            @Override
            public void sampleVolume(SamplerContext samplerContext, DensityBuffer output, DensityVolume volume) {
                DensitySampler.sampleVolumeNaive(samplerContext, output, volume, this);
            }

            @Override
            public float sampleValue(SamplerContext samplerContext, int x, int y, int z) {
                return (float) computeAt(x, y, z);
            }
        };
    }

    @Override
    default Interval range() {
        return Interval.of((float) minValue(), (float) maxValue());
    }

    @Override
    default int domainAxes() {
        return ALL_AXES;
    }

    @Override
    default DensityFunction rewriteChildren(DfRewriteRule rule) {
        return this;
    }
}
