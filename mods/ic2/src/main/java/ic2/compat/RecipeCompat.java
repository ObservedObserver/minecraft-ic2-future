package ic2.compat;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.Level;

public final class RecipeCompat {
  private RecipeCompat() {}

  /** Representative output for the UU recipe graph; crafting still uses Recipe.assemble. */
  public static ItemStack result(Recipe<?> recipe, Level level) {
    return recipe.display().stream()
        .map(display -> display.result().resolveForFirstStack(SlotDisplayContext.fromLevel(level)))
        .filter(stack -> !stack.isEmpty())
        .findFirst()
        .orElse(ItemStack.EMPTY);
  }
}
