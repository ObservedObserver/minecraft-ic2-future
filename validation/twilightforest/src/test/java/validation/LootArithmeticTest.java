package validation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import org.junit.jupiter.api.Test;
import twilightforest.loot.runtime.LootArithmetic;
import static org.junit.jupiter.api.Assertions.*;

class LootArithmeticTest {
    @Test
    void defaultIntegerConversionMatchesActual2612MathRoundIncludingNegativeHalfTies() {
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -2.75F, -2.5F, -2.25F, -0.75F, -0.5F, -0.25F, 0, 0.49F, 0.5F, 1.5F, 5.9F}) {
            assertEquals(Math.round(value), LootArithmetic.asLegacyInt(ContextFloatProviders.exactly(value)).value().getInt(null), "value=" + value);
        }
        assertEquals(0, LootArithmetic.asLegacyInt(ContextFloatProviders.exactly(-0.5F)).value().getInt(null));
        assertEquals(-1, LootArithmetic.asLegacyInt(ContextFloatProviders.exactly(-0.75F)).value().getInt(null));
    }

    @Test
    void largeFiniteRoundedIntegerDoesNotLosePrecisionThroughAnIntermediateFloat() {
        assertEquals(Integer.MAX_VALUE, LootArithmetic.asLegacyInt(ContextFloatProviders.exactly(Float.MAX_VALUE)).value().getInt(null));
        assertEquals(Integer.MIN_VALUE, LootArithmetic.asLegacyInt(ContextFloatProviders.exactly(-Float.MAX_VALUE)).value().getInt(null));
    }

    @Test
    void multiplayerRollsDrawBaseFirstAndConsumeEveryAdditionalPlayerDrawEvenIfNegative() {
        var calls = new ArrayList<String>();
        var draw = new AtomicInteger();
        float total = LootArithmetic.sumRolls(4, () -> { calls.add("base"); return 2.25F; }, () -> {
            calls.add("extra"); return new float[]{-3, 1.5F, 0.5F}[draw.getAndIncrement()];
        });
        assertEquals(4.25F, total);
        assertEquals(List.of("base", "extra", "extra", "extra"), calls);
    }

    @Test
    void zeroOrOneQualifiedPlayerKeepsBaseAndDoesNotDrawExtras() {
        for (int players : new int[]{0, 1}) {
            assertEquals(-1.5F, LootArithmetic.sumRolls(players, () -> -1.5F, () -> { fail("unexpected extra draw"); return 0; }));
        }
    }

    @Test
    void extraItemCountUsesOneSharedIntegerDrawForAllExtraPlayers() {
        var calls = new AtomicInteger();
        assertEquals(12, LootArithmetic.extraItems(5, () -> { calls.incrementAndGet(); return 3; }));
        assertEquals(1, calls.get());
        assertEquals(-6, LootArithmetic.extraItems(4, () -> -2));
        assertEquals(0, LootArithmetic.extraItems(1, () -> { fail("unexpected draw"); return 0; }));
    }

    @Test
    void integerUniformProviderRetainsAnIntegerUpperEndpoint() {
        var provider = assertInstanceOf(net.minecraft.world.level.storage.loot.providers.number.ints.UniformGenerator.class,
            ContextIntProviders.between(2, 4).value());
        assertEquals(2, provider.min().value().getInt(null));
        assertEquals(4, provider.max().value().getInt(null));
    }
    @Test
    void exactRoundingCodecBindsNativeFloatInput() {
        var json = com.google.gson.JsonParser.parseString("{\"input\":-2.5}");
        var rounded = twilightforest.loot.runtime.LegacyRounded.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(-2, rounded.getInt(null));
        var encoded = twilightforest.loot.runtime.LegacyRounded.CODEC.codec().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, rounded).getOrThrow();
        assertEquals(-2, twilightforest.loot.runtime.LegacyRounded.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, encoded).getOrThrow().getInt(null));
    }
}
