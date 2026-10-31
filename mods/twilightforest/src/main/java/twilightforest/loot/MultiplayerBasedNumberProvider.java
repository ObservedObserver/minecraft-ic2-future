package twilightforest.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import twilightforest.config.TFConfig;
import twilightforest.init.TFDataAttachments;
import twilightforest.loot.runtime.LootArithmetic;

public record MultiplayerBasedNumberProvider(Holder<ContextFloatProvider> rollsPerPlayer, Holder<ContextFloatProvider> defaultRolls) implements ContextFloatProvider {
    public static final MapCodec<MultiplayerBasedNumberProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        ContextFloatProviders.CODEC.fieldOf("per_player_rolls").forGetter(MultiplayerBasedNumberProvider::rollsPerPlayer),
        ContextFloatProviders.CODEC.fieldOf("default_rolls").forGetter(MultiplayerBasedNumberProvider::defaultRolls)
    ).apply(i, MultiplayerBasedNumberProvider::new));
    @Override public MapCodec<MultiplayerBasedNumberProvider> codec() { return CODEC; }
    public static Holder<ContextFloatProvider> rollsForPlayers(Holder<ContextFloatProvider> extra, Holder<ContextFloatProvider> base) {
        return Holder.direct(new MultiplayerBasedNumberProvider(extra, base));
    }
    public static Holder<ContextIntProvider> integerRollsForPlayers(Holder<ContextFloatProvider> extra, Holder<ContextFloatProvider> base) {
        return LootArithmetic.asLegacyInt(rollsForPlayers(extra, base));
    }
    @Override public float getFloatUnsafe(LootContext context) {
        var entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (TFConfig.multiplayerFightAdjuster.adjustsLootRolls() && entity != null && entity.hasData(TFDataAttachments.MULTIPLAYER_FIGHT)) {
            int players = entity.getData(TFDataAttachments.MULTIPLAYER_FIGHT).getQualifiedPlayers().size();
            return LootArithmetic.sumRolls(players, () -> defaultRolls.value().getFloat(context), () -> rollsPerPlayer.value().getFloat(context));
        }
        return defaultRolls.value().getFloat(context);
    }
    @Override public void validate(ValidationContext context) {
        Validatable.validateHolder(context, "per_player_rolls", rollsPerPlayer);
        Validatable.validateHolder(context, "default_rolls", defaultRolls);
    }
}
