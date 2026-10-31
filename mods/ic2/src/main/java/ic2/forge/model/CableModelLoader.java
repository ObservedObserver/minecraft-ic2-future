package ic2.forge.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import ic2.core.block.wiring.CableFoam;
import ic2.core.block.wiring.CableType;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;

public record CableModelLoader(CableType cableType, int insulation, String foam, boolean active)
    implements Ic2Model {
  public static final MapCodec<CableModelLoader> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      Codec.STRING
                          .xmap(CableType::valueOf, CableType::name)
                          .fieldOf("cable_type")
                          .forGetter(CableModelLoader::cableType),
                      Codec.intRange(0, 3)
                          .fieldOf("insulation")
                          .forGetter(CableModelLoader::insulation),
                      Codec.STRING
                          .optionalFieldOf("foam", "none")
                          .forGetter(CableModelLoader::foam),
                      Codec.BOOL
                          .optionalFieldOf("active", false)
                          .forGetter(CableModelLoader::active))
                  .apply(instance, CableModelLoader::new));

  @Override
  public MapCodec<CableModelLoader> codec() {
    return CODEC;
  }

  @Override
  public BlockStateModel bake(ModelBaker baker) {
    return new DynamicCableModelForge(cableType, insulation, CableFoam.get(foam), active, baker);
  }

  @Override
  public void resolveDependencies(ResolvableModel.Resolver resolver) {}
}
