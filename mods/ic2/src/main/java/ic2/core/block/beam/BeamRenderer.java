package ic2.core.block.beam;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class BeamRenderer extends EntityRenderer<ParticleEntity, EntityRenderState> {
  private static final Identifier TEXTURE =
      Identifier.fromNamespaceAndPath("ic2", "textures/models/beam.png");

  public BeamRenderer(Context context) {
    super(context);
  }

  @Override
  public EntityRenderState createRenderState() {
    return new EntityRenderState();
  }

  @Override
  public void submit(
      EntityRenderState state,
      PoseStack poses,
      SubmitNodeCollector collector,
      CameraRenderState camera) {
    poses.pushPose();
    poses.rotate(camera.orientation);
    collector.submitCustomGeometry(
        poses,
        RenderTypes.entityTranslucent(TEXTURE),
        (pose, vertices) -> {
          for (int i = 0; i < 4; i++) {
            float x = i < 2 ? -0.1F : 0.1F;
            float y = i != 0 && i != 3 ? 0.1F : -0.1F;
            vertices
                .addVertex(pose, x, y, 0)
                .setColor(-1)
                .setUv(i < 2 ? 0 : 1, i != 0 && i != 3 ? 1 : 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(state.lightCoords)
                .setNormal(pose, 0, 1, 0);
          }
        });
    poses.popPose();
    super.submit(state, poses, collector, camera);
  }
}
