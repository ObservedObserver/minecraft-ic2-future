package ic2.compat;

import net.minecraft.world.item.crafting.CraftingRecipe;

public interface Ic2CraftingRecipe extends CraftingRecipe {
  java.util.List<net.minecraft.world.item.crafting.Ingredient> getIngredients();

  net.minecraft.world.item.ItemStack getResultItem(
      net.minecraft.core.HolderLookup.Provider registries);

  net.minecraft.world.item.ItemStack assemble(
      net.minecraft.world.item.crafting.CraftingInput input,
      net.minecraft.core.HolderLookup.Provider registries);

  boolean canCraftInDimensions(int width, int height);

  @Override
  default net.minecraft.world.item.ItemStack assemble(
      net.minecraft.world.item.crafting.CraftingInput input) {
    return assemble(input, net.minecraft.core.RegistryAccess.EMPTY);
  }

  default java.util.List<java.util.Optional<net.minecraft.world.item.crafting.Ingredient>>
      getOptionalIngredients() {
    return getIngredients().stream().map(java.util.Optional::ofNullable).toList();
  }

  @Override
  default boolean showNotification() {
    return true;
  }

  @Override
  default String group() {
    return "";
  }

  @Override
  default net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
    return net.minecraft.world.item.crafting.PlacementInfo.createFromOptionals(
        getOptionalIngredients());
  }

  @Override
  default java.util.List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
    net.minecraft.world.item.ItemStack result =
        getResultItem(net.minecraft.core.RegistryAccess.EMPTY);
    if (result.isEmpty() || isSpecial()) return java.util.List.of();
    var resultDisplay =
        new net.minecraft.world.item.crafting.display.SlotDisplay.ItemStackSlotDisplay(
            net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(result));
    var station =
        new net.minecraft.world.item.crafting.display.SlotDisplay.ItemSlotDisplay(
            net.minecraft.world.item.Items.CRAFTING_TABLE);
    var inputs =
        getOptionalIngredients().stream()
            .map(
                value ->
                    value
                        .map(net.minecraft.world.item.crafting.Ingredient::display)
                        .orElse(
                            net.minecraft.world.item.crafting.display.SlotDisplay.Empty.INSTANCE))
            .toList();
    if (this instanceof ic2.core.recipe.AdvShapelessRecipe) {
      return java.util.List.of(
          new net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay(
              inputs, resultDisplay, station));
    }
    return java.util.List.of(
        new net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay(
            getIc2RecipeWidth(), getIc2RecipeHeight(), inputs, resultDisplay, station));
  }

  int getIc2RecipeWidth();

  int getIc2RecipeHeight();

  default int getHeight() {
    return this.getIc2RecipeHeight();
  }

  default int getWidth() {
    return this.getIc2RecipeWidth();
  }
}
