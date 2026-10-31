package twilightforest.world.components.chunkgenerators;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.codec.RegistryFileCodec;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import org.jetbrains.annotations.NotNull;
import twilightforest.TFRegistries;
import twilightforest.world.components.layer.BiomeDensitySource;

/**
 * A DensityFunction implementation that enables Biomes to influence terrain formulations, if in the noise chunk generator.
 */
public class NoiseDensityRouter implements DensityFunction {
	public static final MapCodec<NoiseDensityRouter> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
		RegistryFileCodec.create(TFRegistries.Keys.BIOME_TERRAIN_DATA, BiomeDensitySource.CODEC, false).fieldOf("terrain_source").forGetter(NoiseDensityRouter::biomeDensitySourceHolder),
		Codec.doubleRange(-64, 0).fieldOf("lower_density_bound").forGetter(NoiseDensityRouter::lowerDensityBound),
		Codec.doubleRange(0, 64).fieldOf("upper_density_bound").forGetter(NoiseDensityRouter::upperDensityBound),
		Codec.doubleRange(0, 32).orElse(8.0).fieldOf("depth_scalar").forGetter(NoiseDensityRouter::depthScalar)
	).apply(inst, NoiseDensityRouter::new));

	private final Holder<BiomeDensitySource> biomeDensitySourceHolder;
	private final double lowerDensityBound;
	private final double upperDensityBound;
	private final double depthScalar;

	/**
	 * @param biomeDensitySource A BiomeDensitySource containing TerrainColumns, providing per-biome scaling and depth behavior that allows biomes to distinguish their landscapes.
	 * @param lowerDensityBound  Lower clamp bound
	 * @param upperDensityBound  Upper clamp bound
	 */
	public NoiseDensityRouter(Holder<BiomeDensitySource> biomeDensitySource, double lowerDensityBound, double upperDensityBound, double depthScalar) {
		this.biomeDensitySourceHolder = biomeDensitySource;
		this.lowerDensityBound = lowerDensityBound;
		this.upperDensityBound = upperDensityBound;
		this.depthScalar = depthScalar;
	}

    @Override
    public net.minecraft.world.level.levelgen.densityfunction.DensitySampler compileSampler(CompileContext context) {
        var source = biomeDensitySourceHolder.value();
        var compiled = source.compile(context);
        return new ColumnDensitySampler<>((source.domainAxes() & AXIS_Y) == 0, (samplerContext, x, y, z) -> compiled.sampleTerrain(x, y, z, samplerContext),
            (data, samplerContext, x, y, z) -> (float) data.scale);
    }
    @Override public net.minecraft.util.Interval range() { return net.minecraft.util.Interval.of((float) lowerDensityBound, (float) upperDensityBound); }
    @Override public int domainAxes() { return biomeDensitySourceHolder.value().domainAxes(); }
    @Override public DensityFunction rewriteChildren(net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule rule) {
        return new NoiseDensityRouter(Holder.direct(biomeDensitySourceHolder.value().rewriteChildren(rule)), lowerDensityBound, upperDensityBound, depthScalar);
    }
    @Override public MapCodec<NoiseDensityRouter> codec() { return CODEC; }

	public Holder<BiomeDensitySource> biomeDensitySourceHolder() {
		return this.biomeDensitySourceHolder;
	}

	public double lowerDensityBound() {
		return this.lowerDensityBound;
	}

	public double upperDensityBound() {
		return this.upperDensityBound;
	}

	public double depthScalar() {
		return this.depthScalar;
	}

}
