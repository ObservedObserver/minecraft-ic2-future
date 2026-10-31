from pathlib import Path
root=Path('src/main/java')
p=root/'ic2/core/item/armor/jetpack/LayerJetpackOverride.java'
p.write_text('''package ic2.core.item.armor.jetpack;

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
  private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
      "ic2", "textures/models/armor/ic2_jet_pack_layer_1.png");

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
  public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
      HumanoidRenderState state, float yRot, float xRot) {
    ItemStack chest = state.chestEquipment;
    if (!JetpackHandler.hasJetpackAttached(chest)) return;
    collector.submitCustomGeometry(poses,
        chest.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(TEXTURE)
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
''')
def edit(name, changes):
 p=root/name;s=p.read_text()
 for a,b in changes:
  if a not in s: print('missing',name,a[:65])
  s=s.replace(a,b)
 p.write_text(s)
edit('ic2/forge/ClientEventHandlerForge.java',[('import net.neoforged.neoforge.client.event.RenderHighlightEvent;\n','')])
edit('ic2/core/gui/GuiElement.java',[('InventoryMenu.BLOCK_ATLAS','TextureAtlas.LOCATION_BLOCKS')])
edit('ic2/core/Ic2Gui.java',[
 ('super(container, playerInventory, title);\n    this.imageHeight = ySize;\n    this.imageWidth = xSize;', 'super(container, playerInventory, title, xSize, ySize);'),
 ('.renderLeftAlignedNoShadow(guiGraphics, x, y, 10, color);','.visitLines(net.minecraft.client.gui.TextAlignment.LEFT, x, y, 10,\n            (lineX, lineY, line) -> guiGraphics.text(this.font, line, lineX, lineY, color, false));'),
 ('    this.drawForegroundLayer(guiGraphics, mouseX - this.leftPos, mouseY - this.topPos);\n    this.flushTooltips(guiGraphics);','''    guiGraphics.pose().pushMatrix();
    guiGraphics.pose().translate(-this.leftPos, -this.topPos);
    try {
      this.drawForegroundLayer(guiGraphics, mouseX - this.leftPos, mouseY - this.topPos);
      this.flushTooltips(guiGraphics);
    } finally {
      guiGraphics.pose().popMatrix();
    }'''),
 ('if (scrollDelta != 0.0)', 'if (scrollY != 0.0)'),('direction = scrollDelta < 0.0','direction = scrollY < 0.0'),
 ('super.mouseScrolled(mouseX, mouseY, 0, scrollDelta)','super.mouseScrolled(mouseX, mouseY, scrollDelta, scrollY)')])
edit('ic2/core/gui/dynamic/GuiParser.java', [('Screen.hasAltDown()', 'net.minecraft.client.Minecraft.getInstance().hasAltDown()'),('Screen.hasControlDown()', 'net.minecraft.client.Minecraft.getInstance().hasControlDown()')])
edit('ic2/core/sound/SoundManagerClient.java',[('sound.getLocation()', 'sound.getIdentifier()'),('getSoundManager().pause()', 'getSoundManager().pauseAllExcept()')])
edit('ic2/core/GuiOverlayer.java',[('        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);\n','')])
edit('ic2/core/gui/VanillaButton.java',[('guiGraphics.blitSprite(\n', 'guiGraphics.blitSprite(\n        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ')])
p=root/'ic2/core/gui/TextBox.java';s=p.read_text();s='\n'.join(l for l in s.split('\n') if not(l.startswith('import com.mojang.') or l=='import net.minecraft.client.renderer.GameRenderer;'))
s=s.replace('this.drawHighlightedArea(xCursorPos', 'this.drawHighlightedArea(guiGraphics, xCursorPos')
a=s.index('  protected void drawHighlightedArea(');b=s.index('  @Override',a)
s=s[:a]+'''  protected void drawHighlightedArea(GuiGraphicsExtractor graphics,
      int startX, int startY, int endX, int endY) {
    int left = this.gui.getX();
    int top = this.gui.getY();
    int maxX = left + this.x + this.width;
    graphics.textHighlight(Math.min(maxX, left + Math.min(startX, endX)),
        top + Math.min(startY, endY), Math.min(maxX, left + Math.max(startX, endX)),
        top + Math.max(startY, endY), true);
  }

'''+s[b:]
for old,key in [('SelectAll','A'),('Copy','C'),('Paste','V'),('Cut','X')]:
 s=s.replace('Screen.is'+old+'(keyCode)', '(SideProxyClient.mc.hasControlDown() && keyCode == com.mojang.blaze3d.platform.InputConstants.KEY_'+key+')')
for number,key in [(259,'BACKSPACE'),(260,'INSERT'),(264,'DOWN'),(265,'UP'),(266,'PAGEUP'),(267,'PAGEDOWN'),(261,'DELETE'),(262,'RIGHT'),(263,'LEFT'),(268,'HOME'),(269,'END')]:
 s=s.replace('case '+str(number)+':','case com.mojang.blaze3d.platform.InputConstants.KEY_'+key+':')
# GUI elements receive coordinates relative to the container, just like their background.
s=s.replace(' - this.gui.getX();',';').replace(' - this.gui.getY();',';')
p.write_text(s)
