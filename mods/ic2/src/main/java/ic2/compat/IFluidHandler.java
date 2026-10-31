package ic2.compat;

import net.neoforged.neoforge.fluids.FluidStack;

/** IC2's simulation-style fluid operations, backed by NeoForge transactions at the boundary. */
public interface IFluidHandler {
  int getTanks();

  FluidStack getFluidInTank(int tank);

  int getTankCapacity(int tank);

  boolean isFluidValid(int tank, FluidStack stack);

  int fill(FluidStack stack, FluidAction action);

  FluidStack drain(FluidStack stack, FluidAction action);

  FluidStack drain(int amount, FluidAction action);

  enum FluidAction {
    EXECUTE,
    SIMULATE;

    public boolean simulate() {
      return this == SIMULATE;
    }

    public boolean execute() {
      return this == EXECUTE;
    }
  }
}
