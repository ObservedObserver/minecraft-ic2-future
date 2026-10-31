package validation;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import twilightforest.loot.runtime.LegacyCompostLayers;
import static org.junit.jupiter.api.Assertions.*;

class CompostLayersTest {
    @Test void allLegacyProbabilitiesRetainFirstLayerAndDoubleDrawSequence() {
        for(float chance:new float[]{0.1F,0.3F,0.5F,0.65F,0.85F}) for(int level:new int[]{0,1,6}) {
            var expected=RandomSource.create(-914201L);var actual=RandomSource.create(-914201L);
            for(int i=0;i<2048;i++) {
                int oldResult=(level==0 && chance>0 || expected.nextDouble()<(double)chance)?1:0;
                assertEquals(oldResult,LegacyCompostLayers.sample(chance,level,actual));
                assertEquals(expected.nextLong(),actual.nextLong());
            }
        }
    }
    @Test void generatedLayerProvidersDecodeWithOriginalFloatProbabilities() throws Exception {
        var root=java.nio.file.Path.of(System.getProperty("twilightforest.projectRoot"),"src/generated/resources/data/twilightforest/context_int_provider/compostable");
        for(int chance:new int[]{10,30,50,65,85}) {
            var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("legacy_"+chance+".json")));
            var provider=LegacyCompostLayers.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE,json).getOrThrow();
            assertEquals(chance/100.0F,provider.chance());
        }
    }
}
