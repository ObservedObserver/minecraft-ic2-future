package ic2.compat;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public interface Tier {
  int getUses();

  float getSpeed();

  float getAttackDamageBonus();

  TagKey<Block> getIncorrectBlocksForDrops();

  int getEnchantmentValue();

  Ingredient getRepairIngredient();

  TagKey<Item> getRepairItems();

  default ToolMaterial asToolMaterial() {
    return new ToolMaterial(
        getIncorrectBlocksForDrops(),
        getUses(),
        getSpeed(),
        getAttackDamageBonus(),
        getEnchantmentValue(),
        getRepairItems());
  }
}
