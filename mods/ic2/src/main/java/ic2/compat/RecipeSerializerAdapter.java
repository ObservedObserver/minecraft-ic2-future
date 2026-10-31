package ic2.compat;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Keeps each IC2 codec paired with the single registered vanilla serializer instance. */
public abstract class RecipeSerializerAdapter<T extends Recipe<?>> {
  private RecipeSerializer<T> serializer;

  public abstract MapCodec<T> codec();

  public abstract StreamCodec<RegistryFriendlyByteBuf, T> streamCodec();

  public final RecipeSerializer<T> serializer() {
    if (serializer == null) {
      serializer = new RecipeSerializer<>(codec(), streamCodec());
    }
    return serializer;
  }
}
