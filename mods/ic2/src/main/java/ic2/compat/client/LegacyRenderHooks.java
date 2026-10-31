package ic2.compat.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Adapts IC2's existing callbacks to serializable native item properties and tint sources. */
public final class LegacyRenderHooks {
  private LegacyRenderHooks() {}

  public static final Map<Identifier, Property> GENERIC_PROPERTIES = new ConcurrentHashMap<>();
  public static final Map<Item, Map<Identifier, Property>> ITEM_PROPERTIES =
      new ConcurrentHashMap<>();
  public static final Map<Item, ItemColor> ITEM_COLORS = new ConcurrentHashMap<>();

  @FunctionalInterface
  public interface BlockColor {
    int color(BlockState state, BlockAndTintGetter world, BlockPos pos, int layer);
  }

  @FunctionalInterface
  public interface ItemColor {
    int color(ItemStack stack, int layer);
  }

  @FunctionalInterface
  public interface Property {
    float value(ItemStack stack, ClientLevel level, LivingEntity owner, int seed);
  }

  public static int opaque(int color) {
    return color >>> 24 == 0 ? color | 0xFF000000 : color;
  }

  public static BlockTintSource blockTint(BlockColor callback, int layer) {
    return new BlockTintSource() {
      @Override
      public int color(BlockState state) {
        return opaque(callback.color(state, null, null, layer));
      }

      @Override
      public int colorInWorld(BlockState state, BlockAndTintGetter world, BlockPos pos) {
        return opaque(callback.color(state, world, pos, layer));
      }
    };
  }

  public static int blockColor(
      BlockState state, net.minecraft.world.level.Level world, BlockPos pos, int layer) {
    if (layer < 0) return -1;
    var source =
        net.minecraft.client.Minecraft.getInstance().getBlockColors().getTintSource(state, layer);
    return source == null
        ? -1
        : source.colorInWorld(
            state,
            world instanceof BlockAndTintGetter getter ? getter : BlockAndTintGetter.EMPTY,
            pos);
  }

  public record Numeric(Identifier id) implements RangeSelectItemModelProperty {
    public static final MapCodec<Numeric> CODEC =
        Identifier.CODEC.fieldOf("property_id").xmap(Numeric::new, Numeric::id);

    @Override
    public MapCodec<Numeric> type() {
      return CODEC;
    }

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
      var property = ITEM_PROPERTIES.getOrDefault(stack.getItem(), Map.of()).get(id);
      if (property == null) property = GENERIC_PROPERTIES.get(id);
      return property == null
          ? 0
          : Math.clamp(
              property.value(stack, level, owner == null ? null : owner.asLivingEntity(), seed),
              0.0F,
              1.0F);
    }
  }

  public record Tint(int index) implements ItemTintSource {
    public static final MapCodec<Tint> CODEC =
        Codec.intRange(0, 255).optionalFieldOf("index", 0).xmap(Tint::new, Tint::index);

    @Override
    public MapCodec<Tint> type() {
      return CODEC;
    }

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity owner) {
      var callback = ITEM_COLORS.get(stack.getItem());
      return callback == null ? -1 : opaque(callback.color(stack, index));
    }
  }
}
