package twilightforest.world.components.chunkgenerators;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;

public record SqrtDensityFunction(DensityFunction input) implements DensityFunction {
	public static final MapCodec<SqrtDensityFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		DensityFunction.CODEC.fieldOf("input").forGetter(SqrtDensityFunction::input)
	).apply(instance, SqrtDensityFunction::new));

	@Override
	public DensitySampler compileSampler(CompileContext context) {
		return this.input.sqrt().compileSampler(context);
	}

	@Override
	public Interval range() {
		return this.input.sqrt().range();
	}

	@Override
	public int domainAxes() {
		return this.input.domainAxes();
	}

	@Override
	public MapCodec<? extends DensityFunction> codec() {
		return CODEC;
	}

	@Override
	public DensityFunction rewriteChildren(DfRewriteRule rule) {
		return new SqrtDensityFunction(rule.rewrite(this.input));
	}
}
