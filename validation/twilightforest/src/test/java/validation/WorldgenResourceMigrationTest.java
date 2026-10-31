package validation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.OffsetPlacement;
import net.minecraft.world.level.levelgen.densityfunction.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.junit.jupiter.api.Test;
import twilightforest.world.components.chunkgenerators.QuartSampleFunction;
import twilightforest.world.components.chunkgenerators.PointDensityFunction;
import com.mojang.serialization.MapCodec;
import static org.junit.jupiter.api.Assertions.*;

class WorldgenResourceMigrationTest {
    @Test void nativeOffsetRetainsThreeDrawsAndOrderForActualResource() throws Exception {
        var p = Path.of(System.getProperty("twilightforest.projectRoot"),
            "src/generated/resources/data/twilightforest/worldgen/placed_feature/dark_grass.json");
        var modifiers = JsonParser.parseString(Files.readString(p)).getAsJsonObject().getAsJsonArray("placement");
        var json = modifiers.asList().stream().filter(x -> x.getAsJsonObject().get("type").getAsString().equals("minecraft:offset")).findFirst().orElseThrow();
        var modifier = (OffsetPlacement) PlacementModifier.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        for (long seed : new long[]{0, 1, -1, 812398123}) {
            var oldRandom = RandomSource.create(seed); var newRandom = RandomSource.create(seed);
            for (int i=0;i<128;i++) {
                var origin = new BlockPos(-31, 42, -17);
                int x = modifier.x().sample(oldRandom); int y = modifier.y().sample(oldRandom); int z = modifier.x().sample(oldRandom);
                var output = new ArrayList<BlockPos>(); modifier.modify(null, newRandom, origin, output::add);
                assertEquals(java.util.List.of(origin.offset(x,y,z)), output);
                assertEquals(oldRandom.nextLong(), newRandom.nextLong());
            }
        }
    }
    @Test void quartSamplerRetainsFlooringAtNegativeChunkEdgesAndYZero() {
        PointDensityFunction child = new PointDensityFunction() {
            public double computeAt(int x,int y,int z) { return x + y*100 + z*1000; }
            public double minValue() { return -100000; } public double maxValue() { return 100000; }
            public MapCodec<? extends DensityFunction> codec() { throw new UnsupportedOperationException(); }
        };
        var sampler = new QuartSampleFunction(child).compileSampler(null);
        for (int x=-33;x<=33;x++) for (int z=-33;z<=33;z++) for (int y : new int[]{-32,0,223})
            assertEquals((float)(Math.floorDiv(x,4)*4 + Math.floorDiv(z,4)*4000), sampler.sampleValue(null,x,y,z));
    }
    @Test void migratedFlowerNoiseRetainsLegacyNormalizedSamples() throws Exception {
        var p=Path.of(System.getProperty("twilightforest.projectRoot"),"src/generated/resources/data/twilightforest/worldgen/feature/flower_placer.json");
        var root=JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        // The provider is nested in a simple-block feature and must use the new octave codec.
        var noise=root.getAsJsonObject("to_place").getAsJsonObject("noise");
        var actual=NormalNoise.DIRECT_CODEC.parse(JsonOps.INSTANCE,noise).getOrThrow();
        // Native createParity emits a base-amplitude adjustment rather than the optional
        // legacy normalization flag. Compare actual samples, not its JSON representation.
        var legacy=NormalNoise.DIRECT_CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString(
            "{\"base_octave\":0,\"octave_count\":1,\"amplitude_modifiers\":[1.0],\"normalize\":\"legacy\"}")).getOrThrow();
        for(long seed:new long[]{2345L,0L,-19281L}) {
            var expectedNoise=legacy.create(RandomSource.create(seed));
            var actualNoise=actual.create(RandomSource.create(seed));
            for(int x=-128;x<=128;x+=7) for(int z=-128;z<=128;z+=7)
                assertEquals(expectedNoise.get(x/48.0,0,z/48.0),actualNoise.get(x/48.0,0,z/48.0),1.0e-6F);
        }
        assertEquals(0,noise.get("base_octave").getAsInt());
    }
    @Test void spawnRangesKeepInclusiveBoundsAndConsumeEqualEndpointDraws() throws Exception {
        var root=Path.of(System.getProperty("twilightforest.projectRoot"),"src/generated/resources/data/twilightforest/worldgen");
        var counts=new ArrayList<com.google.gson.JsonElement>();
        for (String directory : new String[]{"structure", "biome"}) try (var files=Files.walk(root.resolve(directory))) {
            for (var file:files.filter(p -> p.toString().endsWith(".json")).toList())
                collectSpawnCounts(JsonParser.parseString(Files.readString(file)),counts);
        }
        assertEquals(186,counts.size());
        for (var json:counts) {
            var provider=net.minecraft.util.valueproviders.IntProviders.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            assertTrue(provider.minInclusive()>0);
            var expected=RandomSource.create(89123); var actual=RandomSource.create(89123);
            for (int i=0;i<64;i++) {
                assertEquals(provider.minInclusive()+expected.nextInt(1+provider.maxInclusive()-provider.minInclusive()),provider.sample(actual));
                assertEquals(expected.nextLong(),actual.nextLong());
            }
        }
    }
    private static void collectSpawnCounts(com.google.gson.JsonElement element, ArrayList<com.google.gson.JsonElement> result) {
        if (element.isJsonArray()) { for (var child:element.getAsJsonArray()) collectSpawnCounts(child,result); }
        else if(element.isJsonObject()) {
            var o=element.getAsJsonObject();
            if(o.has("type") && o.has("weight") && o.has("count")) result.add(o.get("count"));
            for(var child:o.entrySet()) collectSpawnCounts(child.getValue(),result);
        }
    }

}
