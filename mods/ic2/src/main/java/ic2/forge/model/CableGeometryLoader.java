package ic2.forge.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import ic2.core.block.wiring.CableFoam;
import ic2.core.block.wiring.CableType;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.neoforged.neoforge.client.model.AbstractUnbakedModel;
import net.neoforged.neoforge.client.model.StandardModelParameters;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/** Keeps procedural cable geometry usable by item models and ordinary model parents. */
public final class CableGeometryLoader implements UnbakedModelLoader<CableGeometryLoader.Model> {
  @Override
  public Model read(JsonObject json, JsonDeserializationContext context) {
    return new Model(
        StandardModelParameters.parse(json, context),
        CableType.valueOf(json.get("type").getAsString()),
        json.get("insulation").getAsInt(),
        CableFoam.get(json.get("foam").getAsString()),
        json.get("active").getAsBoolean());
  }

  public static final class Model extends AbstractUnbakedModel {
    private final CableType type;
    private final int insulation;
    private final CableFoam foam;
    private final boolean active;

    Model(
        StandardModelParameters parameters,
        CableType type,
        int insulation,
        CableFoam foam,
        boolean active) {
      super(parameters);
      this.type = type;
      this.insulation = insulation;
      this.foam = foam;
      this.active = active;
    }

    @Override
    public UnbakedGeometry geometry() {
      return (slots, baker, modelState, name) -> {
        var geometry =
            new DynamicCableModelForge(type, insulation, foam, active, baker).itemGeometry();
        var result = new net.minecraft.client.resources.model.geometry.QuadCollection.Builder();
        var matrix =
            new org.joml.Matrix4f()
                .translation(0.5F, 0.5F, 0.5F)
                .mul(modelState.transformation().getMatrix())
                .translate(-0.5F, -0.5F, -0.5F);
        var transform = new com.mojang.math.Transformation(matrix);
        for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
          for (var quad : geometry.getQuads(side)) {
            result.addCulledFace(
                net.minecraft.core.Direction.rotate(matrix, side),
                transform(quad, transform, modelState));
          }
        }
        for (var quad : geometry.getQuads(null))
          result.addUnculledFace(transform(quad, transform, modelState));
        return result.build();
      };
    }

    private static net.minecraft.client.resources.model.geometry.BakedQuad transform(
        net.minecraft.client.resources.model.geometry.BakedQuad original,
        com.mojang.math.Transformation transformation,
        net.minecraft.client.renderer.block.dispatch.ModelState state) {
      var quad =
          net.neoforged.neoforge.client.model.quad.QuadTransforms.applyTransformation(
              original, transformation);
      var direction =
          net.minecraft.core.Direction.rotate(transformation.getMatrix(), original.direction());
      var material = original.materialInfo();
      var shade = material.shadeDirectionOverride();
      if (shade != null)
        shade = net.minecraft.core.Direction.rotate(transformation.getMatrix(), shade);
      material =
          new net.minecraft.client.resources.model.geometry.BakedQuad.MaterialInfo(
              material.sprite(),
              material.layer(),
              material.itemRenderType(),
              material.itemGlintRenderType(),
              material.itemGlintSpecialRenderType(),
              material.tintIndex(),
              shade,
              material.lightEmission(),
              material.ambientOcclusion());
      long[] uv = new long[4];
      var sprite = material.sprite();
      var uvTransform = state.inverseFaceTransformation(original.direction());
      for (int v = 0; v < 4; v++) {
        float u =
            (net.minecraft.client.model.geom.builders.UVPair.unpackU(original.packedUV(v))
                    - sprite.getU0())
                / (sprite.getU1() - sprite.getU0());
        float w =
            (net.minecraft.client.model.geom.builders.UVPair.unpackV(original.packedUV(v))
                    - sprite.getV0())
                / (sprite.getV1() - sprite.getV0());
        var point = uvTransform.transformPosition(new org.joml.Vector3f(u - 0.5F, w - 0.5F, 0));
        uv[v] =
            net.minecraft.client.model.geom.builders.UVPair.pack(
                sprite.getU(point.x + 0.5F), sprite.getV(point.y + 0.5F));
      }
      return new net.minecraft.client.resources.model.geometry.BakedQuad(
          quad.position0(),
          quad.position1(),
          quad.position2(),
          quad.position3(),
          uv[0],
          uv[1],
          uv[2],
          uv[3],
          direction,
          material,
          quad.bakedNormals(),
          quad.bakedColors());
    }
  }
}
