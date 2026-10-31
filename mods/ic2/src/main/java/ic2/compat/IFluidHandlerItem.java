package ic2.compat;

import net.minecraft.world.item.ItemStack;

public interface IFluidHandlerItem extends IFluidHandler {
  ItemStack getContainer();
}
