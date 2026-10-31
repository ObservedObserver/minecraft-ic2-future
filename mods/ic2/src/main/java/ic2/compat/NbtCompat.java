package ic2.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Reads legacy IC2 tag formats while preserving the old type checks and defaults. */
public final class NbtCompat {
  private NbtCompat() {}

  public static net.minecraft.world.item.ItemStack readItem(
      net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
    return net.minecraft.world.item.ItemStack.OPTIONAL_CODEC
        .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
        .result()
        .orElse(net.minecraft.world.item.ItemStack.EMPTY);
  }

  public static CompoundTag saveItem(
      net.minecraft.world.item.ItemStack stack,
      net.minecraft.core.HolderLookup.Provider registries,
      CompoundTag prefix) {
    Tag encoded =
        net.minecraft.world.item.ItemStack.OPTIONAL_CODEC
            .encodeStart(
                registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
            .getOrThrow();
    return prefix.copy().merge((CompoundTag) encoded);
  }

  public static CompoundTag saveItem(
      net.minecraft.world.item.ItemStack stack,
      net.minecraft.core.HolderLookup.Provider registries) {
    return saveItem(stack, registries, new CompoundTag());
  }

  public static net.neoforged.neoforge.fluids.FluidStack readFluid(
      net.minecraft.core.HolderLookup.Provider registries, CompoundTag tag) {
    return net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC
        .parse(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag)
        .result()
        .orElse(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
  }

  public static CompoundTag saveFluid(
      net.neoforged.neoforge.fluids.FluidStack stack,
      net.minecraft.core.HolderLookup.Provider registries,
      CompoundTag prefix) {
    Tag encoded =
        net.neoforged.neoforge.fluids.FluidStack.OPTIONAL_CODEC
            .encodeStart(
                registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
            .getOrThrow();
    return prefix.copy().merge((CompoundTag) encoded);
  }

  public static boolean contains(CompoundTag tag, String key, int expectedType) {
    Tag value = tag.get(key);
    return value != null
        && (value.getId() == expectedType
            || expectedType == 99 && value.getId() >= 1 && value.getId() <= 6);
  }

  public static ListTag getList(CompoundTag tag, String key, int elementType) {
    ListTag list = tag.getListOrEmpty(key);
    for (Tag entry : list) {
      if (entry.getId() != elementType) {
        return new ListTag();
      }
    }
    return list;
  }
}
