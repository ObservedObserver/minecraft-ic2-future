package ic2.forge.model;

import com.mojang.serialization.MapCodec;
import ic2.core.block.DynamicBeModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;

public record BeModelLoader(Identifier id) implements Ic2Model {
  public static final MapCodec<BeModelLoader> CODEC =
      Identifier.CODEC.fieldOf("id").xmap(BeModelLoader::new, BeModelLoader::id);

  @Override
  public MapCodec<BeModelLoader> codec() {
    return CODEC;
  }

  @Override
  public BlockStateModel bake(ModelBaker baker) {
    return new DynamicBeModelForge(id, baker);
  }

  @Override
  public void resolveDependencies(ResolvableModel.Resolver resolver) {
    Identifier backing = DynamicBeModel.backing(id);
    resolver.markDependency(backing);
    // The active model is a dependency only for machines that have one.
    var blockId =
        Identifier.fromNamespaceAndPath(
            id.getNamespace(), id.getPath().substring(id.getPath().lastIndexOf('/') + 1));
    if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(blockId)
            instanceof ic2.core.block.tileentity.Ic2TileEntityBlock machine
        && machine.canActive()) {
      resolver.markDependency(backing.withSuffix("_active"));
    }
  }
}
