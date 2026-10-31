package ic2.core.block.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import ic2.api.tile.IRotorProvider;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class KineticGeneratorRenderer<T extends BlockEntity & IRotorProvider>
    implements BlockEntityRenderer<T, KineticGeneratorRenderer.State> {
  private static final float DEGREES_PER_TICK = 50;
  private static final Int2ReferenceMap<ModelPart> rotorModels = new Int2ReferenceOpenHashMap<>();

  public static class State extends BlockEntityRenderState {
    public int diameter;
    public float angle;
    public Direction facing;
    public Identifier texture;
  }

  public KineticGeneratorRenderer(Context context) {}

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(
      T rotor,
      State state,
      float partialTick,
      Vec3 cameraPosition,
      ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(
        rotor, state, partialTick, cameraPosition, breakProgress);
    state.diameter = rotor.getRotorDiameter();
    float partial = Minecraft.getInstance().isPaused() ? 0 : partialTick;
    state.angle =
        (rotor.getAngle() + rotor.getRotorAnimationSpeed() * DEGREES_PER_TICK * partial) % 360;
    state.facing = rotor.getFacing();
    state.texture = rotor.getRotorRenderTexture();
    if (rotor.getLevel() != null)
      state.lightCoords =
          LightCoordsUtil.getLightCoords(
              rotor.getLevel(), rotor.getBlockPos().relative(state.facing));
  }

  @Override
  public void submit(
      State state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera) {
    int diameter = state.diameter;
    if (diameter <= 0 || state.texture == null) return;
    ModelPart model = (ModelPart) rotorModels.get(diameter);
    if (model == null) {
      MeshDefinition modelData = new MeshDefinition();
      PartDefinition modelPartData = modelData.getRoot();
      modelPartData.addOrReplaceChild(
          "1",
          CubeListBuilder.create()
              .texOffs(0, 0)
              .addBox(0.0F, 0.0F, -4.0F, 1.0F, diameter * 8, 8.0F),
          PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 0.0F, -0.5F, 0.0F));
      modelPartData.addOrReplaceChild(
          "2",
          CubeListBuilder.create()
              .texOffs(0, 0)
              .addBox(0.0F, 0.0F, -4.0F, 1.0F, diameter * 8, 8.0F),
          PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 3.1F, 0.5F, 0.0F));
      modelPartData.addOrReplaceChild(
          "3",
          CubeListBuilder.create()
              .texOffs(0, 0)
              .addBox(0.0F, 0.0F, -4.0F, 1.0F, diameter * 8, 8.0F),
          PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 4.7F, 0.0F, 0.5F));
      modelPartData.addOrReplaceChild(
          "4",
          CubeListBuilder.create()
              .texOffs(0, 0)
              .addBox(0.0F, 0.0F, -4.0F, 1.0F, diameter * 8, 8.0F),
          PartPose.offsetAndRotation(-8.0F, 0.0F, 0.0F, 1.5F, 0.0F, -0.5F));
      model = LayerDefinition.create(modelData, 32, 256).bakeRoot();
      rotorModels.put(diameter, model);
    }

    matrices.pushPose();
    matrices.translate(0.5, 0.5, 0.5);
    switch (state.facing) {
      case NORTH -> matrices.rotateDegrees(Axis.YP, -90);
      case EAST -> matrices.rotateDegrees(Axis.YP, -180);
      case SOUTH -> matrices.rotateDegrees(Axis.YP, -270);
      case UP -> matrices.rotateDegrees(Axis.ZP, -90);
      default -> {}
    }
    matrices.rotateDegrees(Axis.XP, state.angle);
    matrices.translate(-0.2, 0, 0);
    collector.submitModelPart(
        model,
        matrices,
        RenderTypes.entitySolid(state.texture),
        state.lightCoords,
        OverlayTexture.NO_OVERLAY,
        null);
    matrices.popPose();
  }
}
