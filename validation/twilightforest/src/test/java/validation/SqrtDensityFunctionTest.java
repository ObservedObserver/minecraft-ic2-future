package validation;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Test;
import twilightforest.world.components.chunkgenerators.SqrtDensityFunction;

import static org.junit.jupiter.api.Assertions.*;

class SqrtDensityFunctionTest {
    private static final DensityFunction.CompileContext CONTEXT = new DensityFunction.CompileContext() {
        public Noise createNoiseSampler(Holder<NormalNoise> parameters) { throw new AssertionError("Unexpected noise sampling"); }
        public RandomSource createRandom(Identifier seed) { throw new AssertionError("Unexpected random sampling"); }
        public RandomSource createEndIslandRandom() { throw new AssertionError("Unexpected end-island sampling"); }
    };

    private static float sample(DensityFunction function) {
        return function.compileSampler(CONTEXT).sampleValue(SamplerContext.EMPTY_UNCACHED, -37, 64, 8192);
    }

    @Test
    void compiledSamplerPreservesSquareRootValues() {
        for (float input : new float[]{0, 0.25F, 1, 9, 225}) {
            var function = new SqrtDensityFunction(DensityFunctions.constant(input));
            assertEquals((float)Math.sqrt(input), sample(function), 0.000001F);
            assertEquals(DensityFunction.NO_AXES, function.domainAxes());
        }
    }

    @Test
    void rewriteUsesTheReplacedChild() {
        var function = new SqrtDensityFunction(DensityFunctions.constant(9));
        assertEquals(3, sample(function));
        var rewritten = function.rewriteChildren(ignored -> DensityFunctions.constant(16));
        assertEquals(4, sample(rewritten));
        assertEquals(3, sample(function));
    }

    @Test
    void resourceCodecRetainsInputField() {
        var json = new JsonObject();
        json.addProperty("input", 9);
        var decoded = SqrtDensityFunction.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(3, sample(decoded));
        var encoded = SqrtDensityFunction.CODEC.codec().encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        assertEquals(json, encoded);
    }
}
