package twilightforest.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;

public record LootingEnchantNumberProvider(Holder<Enchantment> enchantment, Holder<ContextFloatProvider> baseValue) implements ContextFloatProvider {
    public static final MapCodec<LootingEnchantNumberProvider> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Enchantment.CODEC.fieldOf("enchantment").forGetter(LootingEnchantNumberProvider::enchantment),
        ContextFloatProviders.CODEC.fieldOf("base_value").forGetter(LootingEnchantNumberProvider::baseValue)
    ).apply(i, LootingEnchantNumberProvider::new));
    @Override public MapCodec<LootingEnchantNumberProvider> codec() { return CODEC; }
    public static Holder<ContextFloatProvider> applyLootingLevelTo(HolderLookup.Provider provider, Holder<ContextFloatProvider> baseValue) {
        return Holder.direct(new LootingEnchantNumberProvider(provider.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), baseValue));
    }
    public static Holder<ContextFloatProvider> applyLootingLevelTo(net.minecraft.core.HolderGetter<Enchantment> enchantments, Holder<ContextFloatProvider> baseValue) {
        return Holder.direct(new LootingEnchantNumberProvider(enchantments.getOrThrow(Enchantments.LOOTING), baseValue));
    }
    @Override public float getFloatUnsafe(LootContext context) {
        if (context.getOptional(LootContextParams.ATTACKING_ENTITY) instanceof LivingEntity living) {
            return EnchantmentHelper.getEnchantmentLevel(enchantment, living) + baseValue.value().getFloat(context);
        }
        return baseValue.value().getFloat(context);
    }
    @Override public void validate(ValidationContext context) { Validatable.validateHolder(context, "base_value", baseValue); }
}
