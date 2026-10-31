package ic2.forge.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;

public record MaskOverlayItemLoader(
    Identifier base, Identifier mask, boolean scaleOverlay, float offset)
    implements ItemModel.Unbaked {
  public static final MapCodec<MaskOverlayItemLoader> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      Identifier.CODEC.fieldOf("base").forGetter(MaskOverlayItemLoader::base),
                      Identifier.CODEC.fieldOf("mask").forGetter(MaskOverlayItemLoader::mask),
                      Codec.BOOL
                          .optionalFieldOf("scale_overlay", true)
                          .forGetter(MaskOverlayItemLoader::scaleOverlay),
                      Codec.FLOAT
                          .optionalFieldOf("offset", 0.001F)
                          .forGetter(MaskOverlayItemLoader::offset))
                  .apply(instance, MaskOverlayItemLoader::new));

  @Override
  public MapCodec<MaskOverlayItemLoader> type() {
    return CODEC;
  }

  @Override
  public void resolveDependencies(ResolvableModel.Resolver resolver) {
    resolver.markDependency(base);
  }

  @Override
  public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
    return new MaskOverlayItemModel(this, context, transformation);
  }
}
