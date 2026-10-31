package ic2.core.ref;

import ic2.compat.Tier;
import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public enum Ic2ToolMaterials implements Tier {
  BRONZE(2, 350, 6.0F, 2.0F, () -> Ingredient.of(Ic2Items.BRONZE_INGOT)),
  CHAINSAW(3, 250, 12.0F, 9.0F, Ingredient::of);

  private final int miningLevel;
  private final int itemDurability;
  private final float miningSpeed;
  private final float attackDamage;
  private final int enchantAbility;
  private final Supplier<Ingredient> repairIngredient;

  Ic2ToolMaterials(
      int miningLevel,
      int itemDurability,
      float miningSpeed,
      float attackDamage,
      Supplier<Ingredient> repairIngredient) {
    this.miningLevel = miningLevel;
    this.itemDurability = itemDurability;
    this.miningSpeed = miningSpeed;
    this.attackDamage = attackDamage;
    this.enchantAbility = 14;
    this.repairIngredient = repairIngredient;
  }

  public int getUses() {
    return this.itemDurability;
  }

  public float getSpeed() {
    return this.miningSpeed;
  }

  public float getAttackDamageBonus() {
    return this.attackDamage;
  }

  public int getLevel() {
    return this.miningLevel;
  }

  @Override
  public @NotNull TagKey<Block> getIncorrectBlocksForDrops() {
    if (this.miningLevel >= 3) {
      return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
    } else if (this.miningLevel >= 2) {
      return BlockTags.INCORRECT_FOR_IRON_TOOL;
    } else {
      return BlockTags.INCORRECT_FOR_STONE_TOOL;
    }
  }

  public int getEnchantmentValue() {
    return this.enchantAbility;
  }

  public net.minecraft.tags.TagKey<net.minecraft.world.item.Item> getRepairItems() {
    return net.minecraft.tags.TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "ic2", "repair/tool_" + name().toLowerCase(java.util.Locale.ROOT)));
  }

  public @NotNull Ingredient getRepairIngredient() {
    return this.repairIngredient.get();
  }
}
