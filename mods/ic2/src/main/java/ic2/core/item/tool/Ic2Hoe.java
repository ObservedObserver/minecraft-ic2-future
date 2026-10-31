package ic2.core.item.tool;

import ic2.compat.Tier;
import net.minecraft.world.item.Item;

public class Ic2Hoe extends Item {
  public Ic2Hoe(Tier material, float attackDamage, float attackSpeed, Properties settings) {
    super(settings.hoe(material.asToolMaterial(), attackDamage, attackSpeed));
  }
}
