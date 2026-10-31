package ic2.core.item;

import ic2.api.reactor.IBaseReactorComponent;
import ic2.api.reactor.IReactor;
import ic2.core.Ic2Potion;
import ic2.core.item.armor.ItemArmorHazmat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemNuclearResource extends Item
    implements IBaseReactorComponent, ic2.compat.LegacyInventoryTicker {
  private final int radiationDuration;
  private final int radiationAmplifier;

  public ItemNuclearResource(Properties settings, int radiationDuration, int radiationAmplifier) {
    super(settings);
    this.radiationDuration = radiationDuration;
    this.radiationAmplifier = radiationAmplifier;
  }

  @Override
  public boolean canBePlacedIn(ItemStack stack, IReactor reactor) {
    return false;
  }

  @Override
  public void inventoryTick(
      net.minecraft.world.item.ItemStack stack,
      net.minecraft.server.level.ServerLevel level,
      net.minecraft.world.entity.Entity owner,
      net.minecraft.world.entity.EquipmentSlot slot) {
    this.inventoryTick(
        stack,
        level,
        owner,
        ic2.compat.InventoryCompat.indexOf(owner, stack, slot),
        slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND);
  }

  public void inventoryTick(
      ItemStack stack, Level world, Entity rawEntity, int slot, boolean selected) {
    if (rawEntity instanceof LivingEntity entity) {
      if (!ItemArmorHazmat.hasCompleteHazmat(entity)) {
        Ic2Potion.radiation.applyTo(entity, this.radiationDuration * 20, this.radiationAmplifier);
      }
    }
  }
}
