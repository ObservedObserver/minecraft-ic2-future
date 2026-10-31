package twilightforest.loot.runtime;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

/** Preserve the old compost chance, guaranteed first layer and double random draw. */
public record LegacyCompostLayers(float chance) implements ContextIntProvider, net.minecraft.world.level.storage.loot.LootContextUser {
    public static final MapCodec<LegacyCompostLayers> CODEC = Codec.floatRange(0,1).fieldOf("chance").xmap(LegacyCompostLayers::new,LegacyCompostLayers::chance);
    public static int sample(float chance, int level, RandomSource random) {
        return (level == 0 && chance > 0 || random.nextDouble() < (double)chance) ? 1 : 0;
    }
    @Override public int getIntUnsafe(LootContext context) {
        var state=context.getOptional(LootContextParams.BLOCK_STATE);
        int level=state != null && state.is(Blocks.COMPOSTER) ? state.getValue(ComposterBlock.LEVEL) : -1;
        return sample(this.chance,level,context.getRandom());
    }
    @Override public java.util.Set<net.minecraft.util.context.ContextKey<?>> getReferencedContextParams() { return java.util.Set.of(LootContextParams.BLOCK_STATE); }
    @Override public MapCodec<LegacyCompostLayers> codec() { return CODEC; }
}
