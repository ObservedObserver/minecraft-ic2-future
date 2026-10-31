package ic2.core.recipe.input;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import ic2.core.util.StackUtil;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class RecipeInputItemStack extends RecipeInputBase {
  public final net.minecraft.world.item.ItemStackTemplate input;

  public RecipeInputItemStack(ItemStack input) {
    this(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(input));
  }

  public RecipeInputItemStack(net.minecraft.world.item.ItemStackTemplate input) {
    this.input = java.util.Objects.requireNonNull(input);
  }

  @Override
  public boolean matches(ItemStack subject) {
    if (this.input.item().value() != subject.getItem()) return false;

    CompoundTag requiredNbt = StackUtil.getTag(this.input.create());
    if (requiredNbt == null || requiredNbt.isEmpty()) return true;

    CompoundTag subjectNbt = StackUtil.getTag(subject);
    if (subjectNbt == null) {
      // Subject has no NBT — check that all required keys map to default values (0 for numbers,
      // etc.)
      for (String key : requiredNbt.keySet()) {
        if (!isDefaultTag(requiredNbt.get(key))) return false;
      }

      return true;
    }

    // Check required ⊆ subject (partial match)
    for (String key : requiredNbt.keySet()) {
      if (!subjectNbt.contains(key)) return false;
      if (!subjectNbt.get(key).equals(requiredNbt.get(key))) return false;
    }

    return true;
  }

  private static boolean isDefaultTag(Tag tag) {
    if (tag instanceof NumericTag num) {
      return num.doubleValue() == 0.0;
    }

    if (tag instanceof StringTag str) {
      return str.value().isEmpty();
    }

    if (tag instanceof ByteArrayTag byteArray) {
      return byteArray.isEmpty();
    }

    if (tag instanceof IntArrayTag intArray) {
      return intArray.isEmpty();
    }

    if (tag instanceof LongArrayTag longArray) {
      return longArray.isEmpty();
    }

    if (tag instanceof ListTag list) {
      return list.isEmpty();
    }

    if (tag instanceof CompoundTag compound) {
      return compound.isEmpty();
    }

    return false;
  }

  @Override
  public int getAmount() {
    return this.input.count();
  }

  @Override
  protected List<ItemStack> listStacks() {
    return Arrays.asList(this.input.create());
  }

  @Override
  public String toString() {
    return "RInputItemStack<"
        + StackUtil.setImmutableSize(this.input.create(), this.getAmount())
        + ">";
  }

  @Override
  public boolean equals(Object obj) {
    RecipeInputItemStack other;
    return obj != null
        && this.getClass() == obj.getClass()
        && this.input.equals((other = (RecipeInputItemStack) obj).input)
        && other.getAmount() == this.getAmount();
  }

  @Override
  public JsonElement toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("item", BuiltInRegistries.ITEM.getKey(this.input.item().value()).toString());
    obj.add(
        "data",
        (JsonElement)
            NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, StackUtil.getTag(this.input.create())));
    if (this.input.count() != 1) {
      obj.addProperty("count", this.input.count());
    }

    return obj;
  }
}
