package twilightforest.world.components.chunkgenerators;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.*;

/** The old FlatCache samples a four-block X/Z grid at Y=0 inside its chunk.
 * This immutable node preserves that grid for the ambient terrain noise, with
 * no shared mutable chunk cache or dependency on evaluation order.
 */
public record QuartSampleFunction(DensityFunction input) implements DensityFunction {
    public static final MapCodec<QuartSampleFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        DensityFunction.CODEC.fieldOf("input").forGetter(QuartSampleFunction::input)
    ).apply(i, QuartSampleFunction::new));

    @Override public DensitySampler compileSampler(CompileContext context) {
        DensitySampler child = this.input.compileSampler(context);
        return new DensitySampler() {
            @Override public float sampleValue(SamplerContext samplerContext, int x, int y, int z) {
                return child.sampleValue(samplerContext, x & ~3, 0, z & ~3);
            }
            @Override public void sampleVolume(SamplerContext samplerContext, DensityBuffer output, DensityVolume volume) {
                DensitySampler.sampleVolumeNaive(samplerContext, output, volume, this);
            }
        };
    }
    @Override public Interval range() { return this.input.range(); }
    @Override public int domainAxes() { return this.input.domainAxes() & (AXIS_X | AXIS_Z); }
    @Override public MapCodec<QuartSampleFunction> codec() { return CODEC; }
    @Override public DensityFunction rewriteChildren(DfRewriteRule rule) {
        DensityFunction rewritten = rule.rewrite(this.input);
        return rewritten == this.input ? this : new QuartSampleFunction(rewritten);
    }
}
