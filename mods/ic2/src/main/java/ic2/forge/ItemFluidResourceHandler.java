package ic2.forge;

import ic2.core.fluid.Ic2FluidItem;
import ic2.core.fluid.Ic2FluidStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.apache.commons.lang3.mutable.MutableObject;

/** Copies and exchanges the complete cell through ItemAccess, so aborts restore item and fluid. */
final class ItemFluidResourceHandler extends ItemAccessResourceHandler<FluidResource> {
  private final Item validItem;

  ItemFluidResourceHandler(ItemAccess access) {
    super(access, 1);
    validItem = access.getResource().getItem();
  }

  private Ic2FluidStack contents(ItemResource resource) {
    if (!resource.is(validItem) || !(validItem instanceof Ic2FluidItem item))
      return Ic2FluidStack.EMPTY;
    return item.drainMb(resource.toStack(), Integer.MAX_VALUE, true, null);
  }

  protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
    if (itemAccess.getAmount() != 1) return FluidResource.EMPTY;
    Ic2FluidStack stack = contents(accessResource);
    return stack == null || stack.isEmpty()
        ? FluidResource.EMPTY
        : FluidResource.of(EnvFluidHandlerForge.getForgeFs(stack));
  }

  protected int getAmountFrom(ItemResource resource, int index) {
    if (itemAccess.getAmount() != 1) return 0;
    Ic2FluidStack stack = contents(resource);
    return stack != null ? stack.getAmountMb() : 0;
  }

  protected int getCapacity(int index, FluidResource resource) {
    return itemAccess.getAmount() == 1 && itemAccess.getResource().is(validItem)
        ? ((Ic2FluidItem) validItem).getCapacityMb(itemAccess.getResource().toStack())
        : 0;
  }

  public boolean isValid(int index, FluidResource resource) {
    if (itemAccess.getAmount() != 1
        || index != 0
        || resource.isEmpty()
        || !itemAccess.getResource().is(validItem)) return false;
    if (resource.equals(getResourceFrom(itemAccess.getResource(), index))) return true;
    Ic2FluidItem item = (Ic2FluidItem) validItem;
    ItemStack stack = itemAccess.getResource().toStack();
    return item.fillMb(
            stack, new Ic2FluidStackImpl(resource.toStack(item.getCapacityMb(stack))), true, null)
        > 0;
  }

  protected ItemResource update(
      ItemResource current, int index, FluidResource fluid, int newAmount) {
    if (!current.is(validItem)) return ItemResource.EMPTY;
    ItemStack stack = current.toStack();
    MutableObject<ItemStack> replacement = new MutableObject<>(stack);
    Ic2FluidItem item = (Ic2FluidItem) validItem;
    int oldAmount = getAmountFrom(current, index);
    if (newAmount < oldAmount) {
      Ic2FluidStack removed = item.drainMb(stack, oldAmount - newAmount, false, replacement);
      if (removed == null || removed.getAmountMb() != oldAmount - newAmount)
        return ItemResource.EMPTY;
    } else if (newAmount > oldAmount) {
      int inserted =
          item.fillMb(
              stack,
              new Ic2FluidStackImpl(fluid.toStack(newAmount - oldAmount)),
              false,
              replacement);
      if (inserted != newAmount - oldAmount) return ItemResource.EMPTY;
    }
    return ItemResource.of(replacement.getValue());
  }
}
