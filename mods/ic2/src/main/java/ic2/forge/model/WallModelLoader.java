package ic2.forge.model;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;

public record WallModelLoader() implements Ic2Model {
  public static final MapCodec<WallModelLoader> CODEC = MapCodec.unit(new WallModelLoader());

  @Override
  public MapCodec<WallModelLoader> codec() {
    return CODEC;
  }

  @Override
  public BlockStateModel bake(ModelBaker baker) {
    return new WallModelForge(baker);
  }

  @Override
  public void resolveDependencies(ResolvableModel.Resolver resolver) {
    for (var color : net.minecraft.world.item.DyeColor.values()) {
      resolver.markDependency(
          ic2.core.IC2.getIdentifier("block/cf/wall_" + color.getSerializedName()));
    }
  }
}
