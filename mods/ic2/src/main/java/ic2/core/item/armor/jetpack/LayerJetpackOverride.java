package ic2.core.item.armor.jetpack;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@OnlyIn(Dist.CLIENT)
public class LayerJetpackOverride
    extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
  private static final Identifier TEXTURE =
      Identifier.fromNamespaceAndPath("ic2", "textures/models/armor/ic2_jet_pack_layer_1.png");

  public LayerJetpackOverride(
      RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> renderer) {
    super(renderer);
  }

  public static void register(EntityRenderersEvent.AddLayers event) {
    for (var skin : event.getSkins()) {
      var renderer = event.getPlayerRenderer(skin);
      if (renderer != null && renderer.getModel() instanceof HumanoidModel) addTo(renderer);
    }
    for (EntityType<?> type : event.getEntityTypes()) {
      if (event.getRenderer(type) instanceof LivingEntityRenderer<?, ?, ?> renderer
          && renderer.getModel() instanceof HumanoidModel) addTo(renderer);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void addTo(LivingEntityRenderer renderer) {
    renderer.addLayer(new LayerJetpackOverride(renderer));
  }

  @Override
  public void submit(
      PoseStack poses,
      SubmitNodeCollector collector,
      int light,
      HumanoidRenderState state,
      float yRot,
      float xRot) {
    ItemStack chest = state.chestEquipment;
    if (!JetpackHandler.hasJetpackAttached(chest)) return;
    collector.submitCustomGeometry(
        poses,
        chest.hasFoil()
            ? RenderTypes.armorCutoutNoCullGlint(TEXTURE)
            : RenderTypes.armorCutoutNoCull(TEXTURE),
        (pose, buffer) -> {
          PoseStack local = new PoseStack();
          local.last().set(pose);
          HumanoidModel<HumanoidRenderState> model = getParentModel();
          model.setupAnim(state);
          List<ModelPart> parts = model.root().getAllParts();
          boolean[] visible = new boolean[parts.size()];
          for (int i = 0; i < parts.size(); i++) {
            visible[i] = parts.get(i).visible;
            parts.get(i).visible = false;
          }
          try {
            model.root().visible = true;
            model.body.visible = model.rightArm.visible = model.leftArm.visible = true;
            model.renderToBuffer(local, buffer, light, OverlayTexture.NO_OVERLAY, -1);
          } finally {
            for (int i = 0; i < parts.size(); i++) parts.get(i).visible = visible[i];
          }
        });
  }
}
