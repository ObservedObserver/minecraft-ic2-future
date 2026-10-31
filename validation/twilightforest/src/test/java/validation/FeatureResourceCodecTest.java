package validation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureResourceCodecTest {
    @Test
    void migratedFernResourcesDecodeThroughNativeFeatureDispatch() throws Exception {
        var root = Path.of(System.getProperty("twilightforest.featureResources"));
        for (String name : new String[]{"dense_ferns.json", "dense_large_ferns.json"}) {
            var json = JsonParser.parseString(Files.readString(root.resolve(name)));
            var feature = Feature.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            assertInstanceOf(SimpleBlockFeature.class, feature);
            assertFalse(json.getAsJsonObject().has("config"));
            var encoded = Feature.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, feature).getOrThrow();
            assertEquals("minecraft:simple_block", encoded.getAsJsonObject().get("type").getAsString());
            var decodedAgain = assertInstanceOf(SimpleBlockFeature.class,
                Feature.DIRECT_CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
            var original = (SimpleBlockFeature) feature;
            assertEquals(original.toPlace().value().getState(null, net.minecraft.util.RandomSource.create(1), net.minecraft.core.BlockPos.ZERO),
                decodedAgain.toPlace().value().getState(null, net.minecraft.util.RandomSource.create(1), net.minecraft.core.BlockPos.ZERO));
        }
    }

    @Test
    void resourceRegistryMovePreservesEveryConfiguredFeaturePath() throws Exception {
        var root = Path.of(System.getProperty("twilightforest.featureResources"));
        assertFalse(Files.exists(root.resolveSibling("configured_feature")));
        try (var paths = Files.walk(root)) {
            assertEquals(129, paths.filter(path -> path.toString().endsWith(".json")).count());
        }
    }
}
