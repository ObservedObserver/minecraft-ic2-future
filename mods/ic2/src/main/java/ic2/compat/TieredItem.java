package ic2.compat;

import net.minecraft.world.item.Item;

public class TieredItem extends Item {
  private final Tier tier;

  public TieredItem(Tier tier, Properties properties) {
    super(properties.repairable(tier.getRepairItems()));
    this.tier = tier;
  }

  public Tier getTier() {
    return tier;
  }
}
