package ic2.core.item;

import net.minecraft.world.item.Item;

public class ItemCoke extends Item {
  public ItemCoke() {
    super(
        new Properties()
            .setId(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.ITEM,
                    ic2.core.IC2.getIdentifier("coke")))
            .stacksTo(64)
            .component(
                net.minecraft.core.component.DataComponents.COOKING_FUEL,
                ic2.compat.FuelCompat.constant(3200)));
  }
}
