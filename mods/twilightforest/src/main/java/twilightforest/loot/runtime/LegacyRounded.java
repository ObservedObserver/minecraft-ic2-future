package twilightforest.loot.runtime;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

/** Exact 26.1.2 NumberProvider default conversion, without an intermediate float round-trip. */
public record LegacyRounded(Holder<ContextFloatProvider> input) implements ContextIntProvider {
    public static final MapCodec<LegacyRounded> CODEC = ContextFloatProviders.CODEC.fieldOf("input").xmap(LegacyRounded::new, LegacyRounded::input);
    @Override public int getIntUnsafe(LootContext context) { return Math.round(input.value().getFloatUnsafe(context)); }
    @Override public MapCodec<LegacyRounded> codec() { return CODEC; }
    @Override public void validate(ValidationContext context) { Validatable.validateHolder(context, "input", input); }
}
