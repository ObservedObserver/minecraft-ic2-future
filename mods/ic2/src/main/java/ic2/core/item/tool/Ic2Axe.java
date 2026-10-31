package ic2.core.item.tool;

import ic2.compat.Tier;
import net.minecraft.world.item.Item;

public class Ic2Axe extends Item {
  public Ic2Axe(Tier material, float attackDamage, float attackSpeed, Properties settings) {
    super(settings.axe(material.asToolMaterial(), attackDamage, attackSpeed));
  }
}
