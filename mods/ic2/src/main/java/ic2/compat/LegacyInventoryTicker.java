package ic2.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface LegacyInventoryTicker {
  void inventoryTick(ItemStack stack, Level level, Entity owner, int slot, boolean selected);
}
