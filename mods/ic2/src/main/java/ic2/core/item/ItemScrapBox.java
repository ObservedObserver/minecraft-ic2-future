package ic2.core.item;

import ic2.api.recipe.Recipes;
import ic2.compat.InteractionResults;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemScrapBox extends Item {
  public ItemScrapBox(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level world, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (world.isClientSide()) {
      return InteractionResults.withStack(InteractionResult.PASS, stack);
    }

    ItemStack drop = Recipes.scrapboxDrops.getDrop(stack, false);
    if (drop == null
        || player.drop(drop, false, net.minecraft.util.Prediction.SERVER_ONLY) == null) {
      return InteractionResults.withStack(InteractionResult.PASS, stack);
    }

    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }
    return InteractionResults.withStack(InteractionResult.SUCCESS, stack);
  }
}
