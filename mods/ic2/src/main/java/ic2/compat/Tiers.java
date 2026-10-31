package ic2.compat;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public enum Tiers implements Tier {
  WOOD(ToolMaterial.WOOD),
  STONE(ToolMaterial.STONE),
  IRON(ToolMaterial.IRON),
  DIAMOND(ToolMaterial.DIAMOND),
  GOLD(ToolMaterial.GOLD),
  NETHERITE(ToolMaterial.NETHERITE);

  private final ToolMaterial material;

  Tiers(ToolMaterial material) {
    this.material = material;
  }

  public int getUses() {
    return material.durability();
  }

  public float getSpeed() {
    return material.speed();
  }

  public float getAttackDamageBonus() {
    return material.attackDamageBonus();
  }

  public TagKey<Block> getIncorrectBlocksForDrops() {
    return material.incorrectBlocksForDrops();
  }

  public int getEnchantmentValue() {
    return material.enchantmentValue();
  }

  public TagKey<Item> getRepairItems() {
    return material.repairItems();
  }

  public Ingredient getRepairIngredient() {
    return Ingredient.of(
        net.minecraft.core.registries.BuiltInRegistries.acquireBootstrapRegistrationLookup(
                net.minecraft.core.registries.BuiltInRegistries.ITEM)
            .getOrThrow(getRepairItems()));
  }

  public ToolMaterial asToolMaterial() {
    return material;
  }
}
