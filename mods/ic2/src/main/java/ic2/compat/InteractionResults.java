package ic2.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public final class InteractionResults {
  private InteractionResults() {}

  public static net.minecraft.world.InteractionResult consume(
      net.minecraft.world.item.ItemStack stack) {
    return net.minecraft.world.InteractionResult.CONSUME.heldItemTransformedTo(stack);
  }

  public static InteractionResult withStack(InteractionResult result, ItemStack stack) {
    return result instanceof InteractionResult.Success success
        ? success.heldItemTransformedTo(stack)
        : result;
  }

  public static InteractionResult pass(ItemStack stack) {
    return InteractionResult.PASS;
  }

  public static InteractionResult fail(ItemStack stack) {
    return InteractionResult.FAIL;
  }

  public static InteractionResult sidedSuccess(ItemStack stack, boolean client) {
    return (client ? InteractionResult.SUCCESS : InteractionResult.CONSUME)
        .heldItemTransformedTo(stack);
  }

  public static ItemStack stackOr(InteractionResult result, ItemStack original) {
    if (result instanceof InteractionResult.Success success
        && success.heldItemTransformedTo() != null) {
      return success.heldItemTransformedTo();
    }
    return original;
  }
}
