package ic2.core.crop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Retains the two intersecting planes used by crops without dedicated block models. */
public class CropStickRenderer
    implements BlockEntityRenderer<TileEntityCrop, CropStickRenderer.State> {
  public static class State extends BlockEntityRenderState {
    public Identifier texture;
  }

  public CropStickRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public State createRenderState() {
    return new State();
  }

  @Override
  public void extractRenderState(
      TileEntityCrop crop,
      State state,
      float partialTick,
      Vec3 cameraPosition,
      ModelFeatureRenderer.CrumblingOverlay breakProgress) {
    BlockEntityRenderer.super.extractRenderState(
        crop, state, partialTick, cameraPosition, breakProgress);
    var card = crop.getCrop();
    var textures = card == null ? java.util.List.<Identifier>of() : card.getTexturesLocation();
    state.texture =
        textures.isEmpty()
            ? null
            : asTextureFile(
                textures.get(Math.clamp(crop.getCurrentAge() - 1, 0, textures.size() - 1)));
  }

  @Override
  public void submit(
      State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
    if (state.texture == null) return;
    collector.submitCustomGeometry(
        poses,
        RenderTypes.entityCutout(state.texture),
        (pose, vertices) -> {
          quad(vertices, pose, 0, 0.001F, 0.5F, 1, 1.001F, 0.5F, state.lightCoords);
          quad(vertices, pose, 0.5F, 0.001F, 0, 0.5F, 1.001F, 1, state.lightCoords);
        });
  }

  private static Identifier asTextureFile(Identifier texture) {
    String path = texture.getPath();
    // CropCard's API retains IC2's old "blocks/" texture convention.
    if (path.startsWith("blocks/")) {
      path = "block/" + path.substring("blocks/".length());
    }
    return Identifier.fromNamespaceAndPath(texture.getNamespace(), "textures/" + path + ".png");
  }

  private static void quad(
      VertexConsumer vertices,
      PoseStack.Pose pose,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      int light) {
    vertex(vertices, pose, x0, y1, z0, 0.0F, 0.0F, light);
    vertex(vertices, pose, x0, y0, z0, 0.0F, 1.0F, light);
    vertex(vertices, pose, x1, y0, z1, 1.0F, 1.0F, light);
    vertex(vertices, pose, x1, y1, z1, 1.0F, 0.0F, light);
  }

  private static void vertex(
      VertexConsumer vertices,
      PoseStack.Pose pose,
      float x,
      float y,
      float z,
      float u,
      float v,
      int light) {
    vertices
        .addVertex(pose.pose(), x, y, z)
        .setColor(255, 255, 255, 255)
        .setUv(u, v)
        .setOverlay(OverlayTexture.NO_OVERLAY)
        .setLight(light)
        .setNormal(pose, 0.0F, 1.0F, 0.0F);
  }
}
