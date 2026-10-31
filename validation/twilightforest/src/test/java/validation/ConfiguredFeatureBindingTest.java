package validation;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.junit.jupiter.api.Test;
import twilightforest.world.components.feature.runtime.ConfiguredFeatureBase;
import twilightforest.world.components.feature.runtime.FeaturePlacementContext;
import twilightforest.world.components.feature.runtime.FeatureType;
import twilightforest.world.components.feature.runtime.NoneFeatureConfiguration;

import static org.junit.jupiter.api.Assertions.*;

class ConfiguredFeatureBindingTest {
    private static final class Algorithm extends ConfiguredFeatureBase<Integer> {
        private final int variant;
        private FeaturePlacementContext<Integer> observed;
        Algorithm(int variant) { super(Codec.INT); this.variant = variant; }
        public boolean place(FeaturePlacementContext<Integer> context) {
            observed = context;
            return context.config() == variant;
        }
    }

    @Test
    void codecBindsConfigurationToTheCorrectVariant() {
        var first = new Algorithm(7);
        var second = new Algorithm(11);
        var json = new JsonObject();
        json.addProperty("config", 7);
        var decoded = first.configuredCodec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertSame(first, decoded.feature());
        assertEquals(7, decoded.config());
        assertSame(first.configuredCodec(), decoded.codec());
        assertNotSame(first.configuredCodec(), second.configuredCodec());
        assertEquals(json, first.configuredCodec().codec().encodeStart(JsonOps.INSTANCE, decoded).getOrThrow());
    }

    @Test
    void nativePlacementPreservesContextAndResult() {
        var algorithm = new Algorithm(7);
        var world = (WorldGenLevel) Proxy.newProxyInstance(WorldGenLevel.class.getClassLoader(),
                new Class<?>[]{WorldGenLevel.class}, (object, method, args) -> {
                    throw new AssertionError("Placement binding must not access world methods");
                });
        var random = RandomSource.create(12345);
        var origin = new BlockPos(-20, 68, 300);
        assertTrue(algorithm.configured(7).place(world, null, random, origin));
        assertSame(world, algorithm.observed.level());
        assertSame(random, algorithm.observed.random());
        assertSame(origin, algorithm.observed.origin());
        assertEquals(7, algorithm.observed.config());
        assertFalse(algorithm.configured(8).place(world, null, random, origin));
    }

    @Test
    void typeFactoryCreatesOneStableCodecPerRegisteredVariant() {
        var registry = DeferredRegister.<MapCodec<? extends Feature>>create(Registries.FEATURE_TYPE, "density_sqrt_validation");
        var calls = new AtomicInteger();
        var type = FeatureType.register(registry, "variant_one", () -> {
            calls.incrementAndGet();
            return new Algorithm(7);
        });
        var other = FeatureType.register(registry, "variant_two", () -> new Algorithm(11));
        assertSame(type.get(), type.get());
        assertEquals(1, calls.get());
        assertNotSame(type.get().configuredCodec(), other.get().configuredCodec());
    }

    @Test
    void emptyConfigurationKeepsItsObjectPayload() {
        var algorithm = new ConfiguredFeatureBase<NoneFeatureConfiguration>(NoneFeatureConfiguration.CODEC) {
            public boolean place(FeaturePlacementContext<NoneFeatureConfiguration> context) { return true; }
        };
        var json = new JsonObject();
        json.add("config", new JsonObject());
        var instance = algorithm.configuredCodec().codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertSame(NoneFeatureConfiguration.NONE, instance.config());
        assertEquals(json, algorithm.configuredCodec().codec().encodeStart(JsonOps.INSTANCE, instance).getOrThrow());
    }
}
