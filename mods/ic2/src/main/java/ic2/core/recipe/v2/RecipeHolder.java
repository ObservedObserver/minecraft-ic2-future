package ic2.core.recipe.v2;

import ic2.api.recipe.MachineRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class RecipeHolder<I, O> implements Recipe<RecipeInput> {
  private final java.util.function.Supplier<MachineRecipe<I, O>> recipe;
  private final Identifier id;
  private final RecipeSerializer<?> serializer;
  private final RecipeType<?> type;

  public RecipeHolder(
      MachineRecipe<I, O> recipe,
      Identifier id,
      RecipeSerializer<?> serializer,
      RecipeType<?> type) {
    this(() -> recipe, id, serializer, type);
  }

  private RecipeHolder(
      java.util.function.Supplier<MachineRecipe<I, O>> recipe,
      Identifier id,
      RecipeSerializer<?> serializer,
      RecipeType<?> type) {
    this.recipe = com.google.common.base.Suppliers.memoize(recipe::get);
    this.id = id;
    this.serializer = serializer;
    this.type = type;
  }

  /** Decode templates during registry load; create actual stacks after component binding. */
  public static <I, O> RecipeHolder<I, O> defer(
      java.util.function.Supplier<RecipeHolder<I, O>> factory,
      RecipeSerializer<?> serializer,
      RecipeType<?> type) {
    return new RecipeHolder<>(() -> factory.get().recipe(), null, serializer, type);
  }

  public MachineRecipe<I, O> recipe() {
    return this.recipe.get();
  }

  public Identifier id() {
    return this.id;
  }

  public RecipeSerializer<?> serializer() {
    return this.serializer;
  }

  public RecipeType<?> type() {
    return this.type;
  }

  public boolean matches(RecipeInput inventory, Level world) {
    return false;
  }

  public ItemStack assemble(RecipeInput inventory) {
    return ItemStack.EMPTY;
  }

  public boolean canCraftInDimensions(int width, int height) {
    return false;
  }

  public ItemStack getResultItem(HolderLookup.Provider registryAccess) {
    return ItemStack.EMPTY;
  }

  public boolean isSpecial() {
    return true;
  }

  public Identifier getId() {
    return this.id;
  }

  public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
    return (RecipeSerializer<? extends Recipe<RecipeInput>>) this.serializer;
  }

  public RecipeType<? extends Recipe<RecipeInput>> getType() {
    return (RecipeType<? extends Recipe<RecipeInput>>) this.type;
  }

  public boolean showNotification() {
    return false;
  }

  public String group() {
    return "";
  }

  public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
    return net.minecraft.world.item.crafting.PlacementInfo.NOT_PLACEABLE;
  }

  public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
    return net.minecraft.world.item.crafting.RecipeBookCategories.CRAFTING_MISC;
  }
}
