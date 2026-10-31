package ic2.forge.model;

import ic2.compat.client.QuadCompat;
import ic2.core.IC2;
import ic2.core.block.misc.WallBlock;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.model.Ic2ModelSnapshot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;

public final class WallModelForge implements DynamicBlockStateModel {
  private static final float OVERLAY_OFFSET = 0.001F;
  private final EnumMap<DyeColor, BlockStateModelPart> bases = new EnumMap<>(DyeColor.class);

  public WallModelForge(ModelBaker baker) {
    for (var color : DyeColor.values())
      bases.put(
          color,
          SimpleModelWrapper.bake(
              baker,
              IC2.getIdentifier("block/cf/wall_" + color.getSerializedName()),
              BlockModelRotation.IDENTITY));
  }

  @Override
  public void collectParts(
      BlockAndTintGetter world,
      BlockPos pos,
      BlockState state,
      RandomSource random,
      List<BlockStateModelPart> output) {
    var snapshot = world.getModelData(pos).get(Ic2ModelSnapshot.PROPERTY);
    var base = bases.get(snapshot == null ? WallBlock.DEFAULT_COLOR : snapshot.wallColor());
    if (snapshot == null) {
      output.add(base);
      return;
    }
    List<List<BakedQuad>> faces = new ArrayList<>(7);
    for (var side : Direction.values()) {
      List<BakedQuad> quads = new ArrayList<>(base.getQuads(side));
      var face = snapshot.obscuration().get(side.ordinal());
      if (face != null) {
        var data = face.data();
        var info = ItemObscurator.getRenderInfo(data.state(), data.side());
        if (info != null && info.uvs.length == data.colorMultipliers().length * 4) {
          for (int texture = 0; texture < data.colorMultipliers().length; texture++) {
            quads.add(
                OverlayRenderUtil.applyBlockOverlay(
                    OverlayRenderUtil.createBlockOverlayQuad(
                        side, OVERLAY_OFFSET, info.sprites[texture]),
                    info,
                    texture,
                    data.colorMultipliers()[texture],
                    side));
          }
        }
      }
      faces.add(List.copyOf(quads));
    }
    faces.add(base.getQuads(null));
    output.add(
        QuadCompat.part(faces, particleMaterial(world, pos, state), base.useAmbientOcclusion()));
  }

  @Override
  public Material.Baked particleMaterial() {
    return bases.get(WallBlock.DEFAULT_COLOR).particleMaterial();
  }

  @Override
  public Material.Baked particleMaterial(BlockAndTintGetter world, BlockPos pos, BlockState state) {
    var snapshot = world.getModelData(pos).get(Ic2ModelSnapshot.PROPERTY);
    if (snapshot == null) return particleMaterial();
    var top = snapshot.obscuration().get(Direction.UP.ordinal());
    if (top != null) {
      var info = ItemObscurator.getRenderInfo(top.state(), top.side());
      if (info != null && info.sprites.length > 0)
        return new Material.Baked(info.sprites[0], false);
    }
    return bases.get(snapshot.wallColor()).particleMaterial();
  }

  @Override
  public int materialFlags() {
    return bases.values().stream()
        .mapToInt(BlockStateModelPart::materialFlags)
        .reduce(0, (a, b) -> a | b);
  }

  @Override
  public int materialFlags(BlockAndTintGetter world, BlockPos pos, BlockState state) {
    List<BlockStateModelPart> parts = new ArrayList<>();
    collectParts(world, pos, state, RandomSource.create(42L), parts);
    return parts.stream().mapToInt(BlockStateModelPart::materialFlags).reduce(0, (a, b) -> a | b);
  }
}
