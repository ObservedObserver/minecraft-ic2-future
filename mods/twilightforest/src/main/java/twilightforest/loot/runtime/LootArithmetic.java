package twilightforest.loot.runtime;

import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Numeric behavior carried forward from TF's old NumberProvider implementations. */
public final class LootArithmetic {
    private LootArithmetic() { }
    @FunctionalInterface public interface FloatDraw { float get(); }
    @FunctionalInterface public interface IntDraw { int get(); }
    public static float sumRolls(int qualifiedPlayers, FloatDraw base, FloatDraw extra) {
        float total = base.get();
        for (int i = 0; i < qualifiedPlayers - 1; i++) total += Math.max(0, extra.get());
        return total;
    }
    public static int extraItems(int qualifiedPlayers, IntDraw extra) {
        return qualifiedPlayers > 1 ? extra.get() * (qualifiedPlayers - 1) : 0;
    }
    public static Holder<ContextIntProvider> asLegacyInt(Holder<ContextFloatProvider> input) {
        // The actual 26.1.2 NumberProvider used Math.round, including negative half ties.
        return Holder.direct(new LegacyRounded(input));
    }
}
