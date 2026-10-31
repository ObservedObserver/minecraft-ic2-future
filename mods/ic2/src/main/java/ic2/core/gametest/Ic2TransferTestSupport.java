package ic2.core.gametest;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Uses the real NeoForge transactional API for the existing fixture assertions. */
final class Ic2TransferTestSupport {
  private Ic2TransferTestSupport() {}

  static ItemStack insert(
      ResourceHandler<ItemResource> handler, int index, ItemStack stack, boolean simulate) {
    try (Transaction transaction = Transaction.openRoot()) {
      int accepted = handler.insert(index, ItemResource.of(stack), stack.getCount(), transaction);
      if (!simulate) transaction.commit();
      return stack.copyWithCount(stack.getCount() - accepted);
    }
  }

  static ItemStack stack(ResourceHandler<ItemResource> handler, int index) {
    return handler.getResource(index).toStack(handler.getAmountAsInt(index));
  }
}
