package ic2.forge;

import com.google.common.collect.MapMaker;
import ic2.core.block.comp.Fluids;
import ic2.core.fluid.Ic2FluidStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Exposes IC2 tanks to the transactional transfer API without changing their internal units. */
final class BlockFluidCapImpl implements ResourceHandler<FluidResource> {
  private static final Map<Fluids.InternalFluidTank, TankJournal> JOURNALS =
      new MapMaker().weakKeys().weakValues().makeMap();
  private final Fluids fluids;
  private final BlockEntity owner;
  private final Direction side;

  BlockFluidCapImpl(Fluids fluids, BlockEntity owner, Direction side) {
    this.fluids = fluids;
    this.owner = owner;
    this.side = side != null ? side : Direction.DOWN;
  }

  private List<Fluids.InternalFluidTank> tanks() {
    List<Fluids.InternalFluidTank> result = new ArrayList<>();
    for (Fluids.InternalFluidTank tank : fluids.getAllTanks()) {
      if (tank.canFill(side) || tank.canDrain(side)) result.add(tank);
    }
    return result;
  }

  private TankJournal journal(Fluids.InternalFluidTank tank) {
    return JOURNALS.computeIfAbsent(tank, key -> new TankJournal(key, owner));
  }

  public int size() {
    return tanks().size();
  }

  public FluidResource getResource(int index) {
    Ic2FluidStack stack = tanks().get(index).getFluidStack();
    return stack == null || stack.isEmpty()
        ? FluidResource.EMPTY
        : FluidResource.of(EnvFluidHandlerForge.getForgeFs(stack));
  }

  public long getAmountAsLong(int index) {
    return tanks().get(index).getFluidAmount();
  }

  public long getCapacityAsLong(int index, FluidResource resource) {
    var tank = tanks().get(index);
    return resource.isEmpty() || tank.canFill(resource.getFluid(), side) ? tank.getCapacity() : 0;
  }

  public boolean isValid(int index, FluidResource resource) {
    return !resource.isEmpty() && tanks().get(index).canFill(resource.getFluid(), side);
  }

  public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
    if (resource.isEmpty() || amount < 0)
      throw new IllegalArgumentException("Invalid fluid insertion");
    var tank = tanks().get(index);
    if (amount == 0 || !tank.canFill(resource.getFluid(), side)) return 0;
    Ic2FluidStack fluid = new Ic2FluidStackImpl(resource.toStack(amount));
    int accepted = tank.fillMb(fluid, true);
    if (accepted == 0) return 0;
    journal(tank).updateSnapshots(transaction);
    return tank.fillMb(fluid.copyWithAmountMb(accepted), false);
  }

  public int extract(
      int index, FluidResource resource, int amount, TransactionContext transaction) {
    if (resource.isEmpty() || amount < 0)
      throw new IllegalArgumentException("Invalid fluid extraction");
    var tank = tanks().get(index);
    if (amount == 0 || !tank.canDrain(side)) return 0;
    Ic2FluidStack fluid = new Ic2FluidStackImpl(resource.toStack(amount));
    int extracted = tank.drainMb(fluid, true);
    if (extracted == 0) return 0;
    journal(tank).updateSnapshots(transaction);
    return tank.drainMb(fluid.copyWithAmountMb(extracted), false);
  }

  private static final class TankJournal extends SnapshotJournal<Ic2FluidStack> {
    private final Fluids.InternalFluidTank tank;
    private final BlockEntity owner;

    TankJournal(Fluids.InternalFluidTank tank, BlockEntity owner) {
      this.tank = tank;
      this.owner = owner;
    }

    protected Ic2FluidStack createSnapshot() {
      return tank.getFluidStack() == null ? null : tank.getFluidStack().copy();
    }

    protected void revertToSnapshot(Ic2FluidStack state) {
      tank.setFluidStack(state == null ? null : state.copy());
    }

    protected void onRootCommit(Ic2FluidStack original) {
      owner.setChanged();
    }
  }
}
