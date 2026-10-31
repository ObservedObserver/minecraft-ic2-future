package ic2.core.recipe.v2;

import ic2.api.recipe.Recipes;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;

public class RecipeManagerGetter<T> implements Recipes.IGetter<T> {
  private final Function<RecipeMap, T> factory;
  private final Map<RecipeMap, T> cache = new WeakHashMap<>();

  public RecipeManagerGetter(Function<RecipeMap, T> factory) {
    this.factory = factory;
  }

  @Override
  public T get(Level world) {
    RecipeMap recipes =
        world instanceof net.minecraft.server.level.ServerLevel serverLevel
            ? serverLevel.recipeAccess().recipeMap()
            : ic2.core.IC2.sideProxy.getRecipeManager();
    return this.cache.computeIfAbsent(recipes, this.factory);
  }
}
