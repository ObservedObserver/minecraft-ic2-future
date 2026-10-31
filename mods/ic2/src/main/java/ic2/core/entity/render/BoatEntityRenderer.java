package ic2.core.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ic2.api.entity.boat.BoatType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractBoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

/** Vanilla boat render-state animation with the IC2 type's texture and water mask. */
public class BoatEntityRenderer extends AbstractBoatRenderer {
  private final EntityModel<BoatRenderState> boatModel;
  private final Model.Simple waterPatch;

  public BoatEntityRenderer(Context context, boolean chest, String modId, BoatType type) {
    super(
        context,
        Identifier.fromNamespaceAndPath(
            modId,
            "textures/entity/" + (chest ? "chest_boat/" : "boat/") + type.getName() + ".png"));
    boatModel =
        new BoatModel(context.bakeLayer(chest ? ModelLayers.OAK_CHEST_BOAT : ModelLayers.OAK_BOAT));
    waterPatch =
        new Model.Simple(
            context.bakeLayer(ModelLayers.BOAT_WATER_PATCH), ignored -> RenderTypes.waterMask());
  }

  @Override
  protected EntityModel<BoatRenderState> model() {
    return boatModel;
  }

  @Override
  protected void submitTypeAdditions(
      BoatRenderState state, PoseStack poses, SubmitNodeCollector collector, int light) {
    if (!state.isUnderWater)
      collector.submitModel(
          waterPatch,
          Unit.INSTANCE,
          poses,
          texture,
          light,
          OverlayTexture.NO_OVERLAY,
          state.outlineColor);
  }
}
