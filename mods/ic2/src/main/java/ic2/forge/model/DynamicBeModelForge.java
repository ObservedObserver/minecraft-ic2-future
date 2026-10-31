package ic2.forge.model;

import ic2.compat.client.QuadCompat;
import ic2.core.block.DynamicBeModel;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.model.Ic2ModelSnapshot;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class DynamicBeModelForge extends DynamicBeModel<List<List<BakedQuad>>> {
  DynamicBeModelForge(Identifier id, ModelBaker baker) {
    super(id, baker);
  }

  static List<BakedQuad> getObscuredQuads(Obscuration.ObscurationData data, Direction targetFace) {
    var refModel = ic2.core.model.ModelUtil.getBlockModel(data.state());
    List<BakedQuad> refQuads = QuadCompat.quads(refModel, data.state(), data.side());
    if (refQuads.isEmpty()) return null;
    List<BakedQuad> result = new ArrayList<>(refQuads.size());
    for (int i = 0; i < refQuads.size(); i++) {
      BakedQuad quad = refQuads.get(i);
      if (data.side() != targetFace) quad = transformQuadFace(quad, data.side(), targetFace);
      if (data.colorMultipliers() != null
          && i < data.colorMultipliers().length
          && data.colorMultipliers()[i] != -1) {
        quad = QuadCompat.tint(quad, data.colorMultipliers()[i]);
      }
      result.add(quad);
    }
    return List.copyOf(result);
  }

  private static BakedQuad transformQuadFace(BakedQuad quad, Direction fromFace, Direction toFace) {
    Vector3fc[] positions = new Vector3fc[4];
    for (int v = 0; v < 4; v++) {
      var old = quad.position(v);
      float[] uv = posToUV(fromFace, old.x(), old.y(), old.z());
      positions[v] = new Vector3f(uvToPos(toFace, uv[0], uv[1]));
    }
    return QuadCompat.withGeometry(
        quad,
        positions,
        toFace,
        BakedNormals.of(BakedNormals.pack(toFace.getStepX(), toFace.getStepY(), toFace.getStepZ())),
        quad.bakedColors());
  }

  private static float[] posToUV(Direction face, float x, float y, float z) {
    return switch (face) {
      case NORTH -> new float[] {x, y};
      case SOUTH -> new float[] {1 - x, y};
      case WEST -> new float[] {1 - z, y};
      case EAST -> new float[] {z, y};
      case DOWN -> new float[] {x, 1 - z};
      case UP -> new float[] {x, z};
    };
  }

  private static float[] uvToPos(Direction face, float u, float v) {
    return switch (face) {
      case NORTH -> new float[] {u, v, 0};
      case SOUTH -> new float[] {1 - u, v, 1};
      case WEST -> new float[] {0, v, 1 - u};
      case EAST -> new float[] {1, v, u};
      case DOWN -> new float[] {u, 0, 1 - v};
      case UP -> new float[] {u, 1, v};
    };
  }

  @Override
  public void collectParts(
      BlockAndTintGetter world,
      BlockPos pos,
      BlockState state,
      RandomSource random,
      List<BlockStateModelPart> output) {
    if (state.getBlock() != this.block) state = this.block.defaultBlockState();
    var snapshot = world.getModelData(pos).get(Ic2ModelSnapshot.PROPERTY);
    boolean active =
        this.block.canActive()
            && (snapshot != null
                ? snapshot.active()
                : state.hasProperty(Ic2TileEntityBlock.ACTIVE)
                    && state.getValue(Ic2TileEntityBlock.ACTIVE));
    List<List<BakedQuad>> mesh = this.getMesh(state, active);
    if (snapshot != null) {
      mesh = new ArrayList<>(mesh);
      for (int face = 0; face < 6; face++) {
        var data = snapshot.obscuration().get(face);
        if (data != null) {
          var obscured = getObscuredQuads(data.data(), Direction.values()[face]);
          if (obscured != null) mesh.set(face, obscured);
        }
      }
    }
    output.add(QuadCompat.part(mesh, this.particleMaterial(), baseModel.useAmbientOcclusion()));
  }

  @Override
  public int materialFlags(BlockAndTintGetter world, BlockPos pos, BlockState state) {
    List<BlockStateModelPart> parts = new ArrayList<>();
    this.collectParts(world, pos, state, RandomSource.create(42L), parts);
    return parts.stream().mapToInt(BlockStateModelPart::materialFlags).reduce(0, (a, b) -> a | b);
  }

  protected List<List<BakedQuad>> generateMesh(
      BlockStateModelPart baseModel, int rot, boolean rotX) {
    RandomSource rand = RandomSource.create(42L);
    List<List<BakedQuad>> mesh = new ArrayList<>(7);

    for (int i = 0; i < 7; i++) mesh.add(Collections.emptyList());

    for (int i = 0; i < 7; i++) {
      Direction face = i < 6 ? Util.ALL_DIRS[i] : null;
      List<BakedQuad> quads = baseModel.getQuads(face);
      int writeIdx = i;
      if (rot != 0) {
        if (face != null) {
          writeIdx = rotateFace(face, rot, rotX).ordinal();
        }

        if (!quads.isEmpty()) {
          List<BakedQuad> newQuads = new ArrayList<>(quads.size());

          for (BakedQuad quad : quads) {
            newQuads.add(rotateQuad(quad, rot, rotX));
          }

          quads = newQuads;
        }
      }

      mesh.set(writeIdx, quads);
    }

    return mesh;
  }
}
