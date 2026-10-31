package ic2.core.recipe.input;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipeInputIngredient extends RecipeInputBase {
  private final Ingredient ingredient;
  private final int amount;

  public RecipeInputIngredient(Ingredient ingredient, int amount) {
    this.ingredient = ingredient;
    this.amount = amount;
  }

  @Override
  public boolean matches(ItemStack subject) {
    return !subject.isEmpty() && this.ingredient.test(subject);
  }

  @Override
  protected List<ItemStack> listStacks() {
    return Arrays.asList(
        this.ingredient.items().map(holder -> new ItemStack(holder)).toArray(ItemStack[]::new));
  }

  @Override
  public int getAmount() {
    return this.amount;
  }

  @Override
  public Ingredient getIngredient() {
    return this.ingredient;
  }

  @Override
  public JsonElement toJson() {
    JsonElement element =
        Ingredient.CODEC
            .encodeStart(ic2.core.recipe.v2.RecipeIo.registryJsonOps(), this.ingredient)
            .getOrThrow();
    if (this.amount == 1) {
      return element;
    } else if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      obj.addProperty("count", this.amount);
      return obj;
    } else {
      JsonObject obj = new JsonObject();
      com.google.gson.JsonArray alternatives = new com.google.gson.JsonArray();
      if (element.isJsonArray()) alternatives.addAll(element.getAsJsonArray());
      else alternatives.add(element);
      obj.add("any", alternatives);
      obj.addProperty("count", this.amount);
      return obj;
    }
  }
}
