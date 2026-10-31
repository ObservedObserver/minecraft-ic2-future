package twilightforest.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import twilightforest.loot.runtime.LootArithmetic;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;
import twilightforest.TwilightForestMod;
import twilightforest.config.TFConfig;
import twilightforest.init.TFDataAttachments;

import java.util.Optional;

public class MultiplayerBasedAdditionLootFunction extends LootItemConditionalFunction {
	public static final MapCodec<MultiplayerBasedAdditionLootFunction> CODEC = RecordCodecBuilder.mapCodec(
		p_298131_ -> commonFields(p_298131_)
			.and(ContextIntProviders.CODEC.fieldOf("extra_count_per_player").forGetter(o -> o.value))
			.apply(p_298131_, MultiplayerBasedAdditionLootFunction::new)
	);

	private final Holder<ContextIntProvider> value;

	public MultiplayerBasedAdditionLootFunction(Optional<Holder<LootItemCondition>> predicates, Holder<ContextIntProvider> value) {
		super(predicates);
		this.value = value;
	}

	public static MultiplayerBasedAdditionLootFunction.Builder addForAllParticipatingPlayers(Holder<ContextIntProvider> additionPerPlayer) {
		return new MultiplayerBasedAdditionLootFunction.Builder(additionPerPlayer);
	}

	@Override
	public MapCodec<? extends LootItemConditionalFunction> codec() {
		return CODEC;
	}

	@Override
	protected ItemStack run(ItemStack stack, LootContext context) {
		if (TFConfig.multiplayerFightAdjuster.adjustsLootRolls()) {
			if (context.hasParameter(LootContextParams.THIS_ENTITY) && context.getOptional(LootContextParams.THIS_ENTITY).hasData(TFDataAttachments.MULTIPLAYER_FIGHT)) {
				int qualifiedPlayers = context.getOptional(LootContextParams.THIS_ENTITY).getData(TFDataAttachments.MULTIPLAYER_FIGHT).getQualifiedPlayers().size();
				if (qualifiedPlayers > 1) {
					int participatingPlayers = qualifiedPlayers - 1;
					int extraItems = LootArithmetic.extraItems(qualifiedPlayers, () -> this.value.value().getInt(context));
					stack.setCount(Mth.clamp(stack.getCount() + extraItems, 0, stack.getMaxStackSize()));
					TwilightForestMod.LOGGER.debug("{} extra players participated in a fight against {}, dropping {} extra {} for a total of {}.", participatingPlayers, context.getOptional(LootContextParams.THIS_ENTITY).getType().getDescription().getString(), extraItems, stack.getItemName().getString(), stack.getCount());
				}
			}
		}
		return stack;
	}

    @Override public void validate(net.minecraft.world.level.storage.loot.ValidationContext context) {
        super.validate(context);
        net.minecraft.world.level.storage.loot.Validatable.validateHolder(context, "extra_count_per_player", value);
    }

	public static class Builder extends LootItemConditionalFunction.Builder<MultiplayerBasedAdditionLootFunction.Builder> {
		private final Holder<ContextIntProvider> count;

		public Builder(Holder<ContextIntProvider> pLootingMultiplier) {
			this.count = pLootingMultiplier;
		}

		@Override
		protected MultiplayerBasedAdditionLootFunction.Builder getThis() {
			return this;
		}

		@Override
		public MultiplayerBasedAdditionLootFunction build() {
			return new MultiplayerBasedAdditionLootFunction(this.getCondition(), this.count);
		}
	}
}
