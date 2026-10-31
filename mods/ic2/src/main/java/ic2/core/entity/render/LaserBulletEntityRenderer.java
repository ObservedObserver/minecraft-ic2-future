package ic2.core.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import ic2.core.IC2;
import ic2.core.entity.LaserBulletEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class LaserBulletEntityRenderer
    extends EntityRenderer<LaserBulletEntity, LaserBulletEntityRenderer.State> {
  private static final Identifier TEXTURE = IC2.getIdentifier("textures/models/laser.png");

  public LaserBulletEntityRenderer(Context context) {
    super(context);
  }

  public static class State extends EntityRenderState {
    public float yaw, pitch;
  }

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(LaserBulletEntity entity, State state, float partialTick) {
    super.extractRenderState(entity, state, partialTick);
    state.yaw = entity.getYRot(partialTick);
    state.pitch = entity.getXRot(partialTick);
  }

  @Override
  public void submit(
      State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
    poses.pushPose();
    poses.rotateDegrees(Axis.YP, state.yaw - 90);
    poses.rotateDegrees(Axis.ZP, state.pitch);
    poses.rotateDegrees(Axis.XP, 45);
    poses.scale(0.05625F, 0.05625F, 0.05625F);
    poses.translate(-4, 0, 0);
    collector.submitCustomGeometry(
        poses,
        RenderTypes.entityCutout(TEXTURE),
        (pose, buffer) -> {
          int light = state.lightCoords;
          this.vertex(pose, buffer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, light);
          this.vertex(pose, buffer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, light);
          this.vertex(pose, buffer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, light);
          this.vertex(pose, buffer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, light);
          this.vertex(pose, buffer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, light);
          this.vertex(pose, buffer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, light);
          this.vertex(pose, buffer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, light);
          this.vertex(pose, buffer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, light);
        });
    for (int side = 0; side < 4; side++) {
      poses.rotateDegrees(Axis.XP, 90);
      collector.submitCustomGeometry(
          poses,
          RenderTypes.entityCutout(TEXTURE),
          (pose, buffer) -> {
            this.vertex(pose, buffer, -8, -2, 0, 0, 0, 0, 1, 0, state.lightCoords);
            this.vertex(pose, buffer, 8, -2, 0, 0.5F, 0, 0, 1, 0, state.lightCoords);
            this.vertex(pose, buffer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, state.lightCoords);
            this.vertex(pose, buffer, -8, 2, 0, 0, 0.15625F, 0, 1, 0, state.lightCoords);
          });
    }
    poses.popPose();
    super.submit(state, poses, collector, camera);
  }

  public void vertex(
      Pose pose,
      VertexConsumer vertexConsumer,
      int x,
      int y,
      int z,
      float u,
      float v,
      int normalX,
      int normalZ,
      int normalY,
      int light) {
    vertexConsumer
        .addVertex(pose, (float) x, (float) y, (float) z)
        .setColor(255, 255, 255, 255)
        .setUv(u, v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, normalX, normalY, normalZ);
  }
}
