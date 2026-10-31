package ic2.compat.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Converts IC2's procedural vertex layout at its boundary to native 26.3 geometry. */
public final class QuadCompat {
  private QuadCompat() {}

  public static BakedQuad fromVertices(
      int[] vertices,
      int tint,
      Direction face,
      TextureAtlasSprite sprite,
      boolean ambientOcclusion) {
    Vector3fc[] positions = new Vector3fc[4];
    long[] uv = new long[4];
    int[] colors = new int[4];
    int[] normals = new int[4];
    int light = 0;
    int stride = vertices.length / 4;
    for (int v = 0; v < 4; v++) {
      int off = v * stride;
      positions[v] =
          new Vector3f(
              Float.intBitsToFloat(vertices[off]),
              Float.intBitsToFloat(vertices[off + 1]),
              Float.intBitsToFloat(vertices[off + 2]));
      colors[v] = ARGB.fromABGR(vertices[off + 3]);
      uv[v] =
          UVPair.pack(
              Float.intBitsToFloat(vertices[off + 4]), Float.intBitsToFloat(vertices[off + 5]));
      light =
          Math.max(light, Math.max((vertices[off + 6] >> 4) & 15, (vertices[off + 6] >> 20) & 15));
      normals[v] = vertices[off + 7];
    }
    var material =
        BakedQuad.MaterialInfo.of(
            new Material.Baked(sprite, false),
            sprite.transparency(),
            tint,
            null,
            light,
            ambientOcclusion);
    return new BakedQuad(
        positions[0],
        positions[1],
        positions[2],
        positions[3],
        uv[0],
        uv[1],
        uv[2],
        uv[3],
        face,
        material,
        BakedNormals.of(normals[0], normals[1], normals[2], normals[3]),
        BakedColors.of(colors[0], colors[1], colors[2], colors[3]));
  }

  public static int[] vertices(BakedQuad quad) {
    int[] result = new int[32];
    for (int v = 0; v < 4; v++) {
      int off = v * 8;
      var pos = quad.position(v);
      result[off] = Float.floatToRawIntBits(pos.x());
      result[off + 1] = Float.floatToRawIntBits(pos.y());
      result[off + 2] = Float.floatToRawIntBits(pos.z());
      result[off + 3] = ARGB.toABGR(quad.bakedColors().color(v));
      result[off + 4] = Float.floatToRawIntBits(UVPair.unpackU(quad.packedUV(v)));
      result[off + 5] = Float.floatToRawIntBits(UVPair.unpackV(quad.packedUV(v)));
      result[off + 6] = quad.materialInfo().lightEmission() * 0x100010;
      result[off + 7] = quad.bakedNormals().normal(v);
    }
    return result;
  }

  public static BakedQuad withGeometry(
      BakedQuad quad,
      Vector3fc[] positions,
      Direction face,
      BakedNormals normals,
      BakedColors colors) {
    var m = quad.materialInfo();
    return new BakedQuad(
        positions[0],
        positions[1],
        positions[2],
        positions[3],
        quad.packedUV0(),
        quad.packedUV1(),
        quad.packedUV2(),
        quad.packedUV3(),
        face,
        m,
        normals,
        colors);
  }

  public static BakedQuad tint(BakedQuad quad, int color) {
    if ((color >>> 24) == 0) color |= 0xFF000000;
    int[] colors = new int[4];
    for (int v = 0; v < 4; v++) colors[v] = ARGB.multiply(quad.bakedColors().color(v), color);
    var m = quad.materialInfo();
    // The sampled tint is now baked into the vertices. Do not tint a second time for the IC2 block.
    var material =
        new BakedQuad.MaterialInfo(
            m.sprite(),
            m.layer(),
            m.itemRenderType(),
            m.itemGlintRenderType(),
            m.itemGlintSpecialRenderType(),
            -1,
            m.shadeDirectionOverride(),
            m.lightEmission(),
            m.ambientOcclusion());
    return new BakedQuad(
        quad.position0(),
        quad.position1(),
        quad.position2(),
        quad.position3(),
        quad.packedUV0(),
        quad.packedUV1(),
        quad.packedUV2(),
        quad.packedUV3(),
        quad.direction(),
        material,
        quad.bakedNormals(),
        BakedColors.of(colors[0], colors[1], colors[2], colors[3]));
  }

  public static QuadCollection collection(List<? extends List<BakedQuad>> faces) {
    QuadCollection.Builder builder = new QuadCollection.Builder();
    for (int face = 0; face < faces.size(); face++) {
      for (BakedQuad quad : faces.get(face)) {
        if (face == 6) builder.addUnculledFace(quad);
        else builder.addCulledFace(Direction.values()[face], quad);
      }
    }
    return builder.build();
  }

  public static BlockStateModelPart part(
      List<? extends List<BakedQuad>> faces, Material.Baked particle, boolean ao) {
    return new SimpleModelWrapper(collection(faces), ao, particle);
  }

  public static List<BakedQuad> quads(
      net.minecraft.client.renderer.block.dispatch.BlockStateModel model,
      net.minecraft.world.level.block.state.BlockState state,
      Direction side) {
    List<BlockStateModelPart> parts = new ArrayList<>();
    model.collectParts(
        net.minecraft.client.renderer.block.BlockAndTintGetter.EMPTY,
        net.minecraft.core.BlockPos.ZERO,
        state,
        net.minecraft.util.RandomSource.create(42L),
        parts);
    List<BakedQuad> quads = new ArrayList<>();
    for (var part : parts) quads.addAll(part.getQuads(side));
    return List.copyOf(quads);
  }
}
