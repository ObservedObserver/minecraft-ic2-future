package ic2.compat;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Adapts external transactional handlers to IC2's existing simulation calls. */
public class TransferFluidView implements IFluidHandlerItem {
  protected final ResourceHandler<FluidResource> handler;
  private final ItemAccess access;

  public TransferFluidView(ResourceHandler<FluidResource> handler, ItemAccess access) {
    this.handler = handler;
    this.access = access;
  }

  public static IFluidHandlerItem forStack(ItemStack stack) {
    // Container operations may replace the Item itself, e.g. an empty cell with a water cell.
    // A forStack access only permits component/count changes on the original Item.
    ItemAccess access =
        ItemAccess.forHandlerIndexStrict(
            VanillaContainerWrapper.ofUncached(new SimpleContainer(stack)), 0);
    ResourceHandler<FluidResource> handler = access.getCapability(Capabilities.Fluid.ITEM);
    return handler != null ? new TransferFluidView(handler, access) : null;
  }

  public int getTanks() {
    return handler.size();
  }

  public FluidStack getFluidInTank(int tank) {
    return handler.getResource(tank).toStack(handler.getAmountAsInt(tank));
  }

  public int getTankCapacity(int tank) {
    return handler.getCapacityAsInt(tank, FluidResource.EMPTY);
  }

  public boolean isFluidValid(int tank, FluidStack stack) {
    return !stack.isEmpty() && handler.isValid(tank, FluidResource.of(stack));
  }

  public int fill(FluidStack stack, FluidAction action) {
    if (stack.isEmpty()) return 0;
    try (Transaction transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
      int accepted = handler.insert(FluidResource.of(stack), stack.getAmount(), transaction);
      if (action.execute()) transaction.commit();
      return accepted;
    }
  }

  public FluidStack drain(FluidStack stack, FluidAction action) {
    if (stack.isEmpty()) return FluidStack.EMPTY;
    try (Transaction transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
      int extracted = handler.extract(FluidResource.of(stack), stack.getAmount(), transaction);
      if (action.execute()) transaction.commit();
      return stack.copyWithAmount(extracted);
    }
  }

  public FluidStack drain(int amount, FluidAction action) {
    if (amount <= 0) return FluidStack.EMPTY;
    for (int index = 0; index < handler.size(); index++) {
      FluidResource fluid = handler.getResource(index);
      if (fluid.isEmpty()) continue;
      FluidStack extracted = drain(fluid.toStack(amount), action);
      if (!extracted.isEmpty()) return extracted;
    }
    return FluidStack.EMPTY;
  }

  public ItemStack getContainer() {
    return access != null ? access.getResource().toStack(access.getAmount()) : ItemStack.EMPTY;
  }
}
