package ic2.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class InventoryCompat {
  private InventoryCompat() {}

  public static int indexOf(Entity owner, ItemStack stack, EquipmentSlot slot) {
    if (owner instanceof Player player) {
      for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
        if (player.getInventory().getItem(index) == stack) return index;
      }
    }
    return slot != null ? slot.getIndex() : -1;
  }

  public static void tickClient(Player player) {
    for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
      ItemStack stack = player.getInventory().getItem(index);
      if (!stack.isEmpty() && stack.getItem() instanceof LegacyInventoryTicker ticker) {
        ticker.inventoryTick(
            stack, player.level(), player, index, player.getMainHandItem() == stack);
      }
    }
  }
}
