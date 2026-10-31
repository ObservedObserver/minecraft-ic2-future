package ic2.core.item.tool;

import ic2.compat.Tier;
import net.minecraft.world.item.Item;

public class Ic2Pickaxe extends Item {
  public Ic2Pickaxe(Tier material, float attackDamage, float attackSpeed, Properties settings) {
    super(settings.pickaxe(material.asToolMaterial(), attackDamage, attackSpeed));
  }
}
