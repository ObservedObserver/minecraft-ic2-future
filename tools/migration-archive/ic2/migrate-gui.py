from pathlib import Path
import re
p=Path('src/main/java/ic2/core/Ic2Gui.java');s=p.read_text()
for imp in ['com.mojang.blaze3d.systems.RenderSystem','com.mojang.blaze3d.vertex.BufferBuilder','com.mojang.blaze3d.vertex.BufferUploader','com.mojang.blaze3d.vertex.DefaultVertexFormat','com.mojang.blaze3d.vertex.PoseStack','com.mojang.blaze3d.vertex.Tesselator','com.mojang.renderpearl.api.vertex.VertexFormat.Mode','net.minecraft.client.renderer.GameRenderer','org.joml.Matrix4f']:
 s=s.replace('import '+imp+';\n','')
s=s.replace('PoseStack matrices','org.joml.Matrix3x2fStack matrices')
s=s.replace('  private GuiGraphicsExtractor guiGraphics;','  private GuiGraphicsExtractor guiGraphics;\n  private static Identifier boundTexture;')
s=s.replace('    RenderSystem.setShaderTexture(0, id);','    boundTexture = id;')
s=s.replace('public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks)', 'public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks)')
s=s.replace('    super.render(guiGraphics, mouseX, mouseY, partialTicks);\n    this.renderTooltip(guiGraphics, mouseX, mouseY);','    super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);')
s=s.replace('  protected void renderBg(GuiGraphicsExtractor guiGraphics, float delta, int mouseX, int mouseY) {','  public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {\n    this.guiGraphics = guiGraphics;\n    super.extractBackground(guiGraphics, mouseX, mouseY, delta);')
s=s.replace('protected final void renderLabels(', 'protected final void extractLabels(')
s=s.replace('    guiGraphics.blit(\n        this.getTextureLocation(),','    guiGraphics.blit(\n        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.getTextureLocation(),')
a=s.index('    Matrix4f matrix = matrices.last().pose();');b=s.index('\n  public void drawSprite(',a)
s=s[:a]+'''    this.guiGraphics.blit(boundTexture, (int) x, (int) y, (int) xE, (int) yE,
        (float) uS, (float) uE, (float) vS, (float) vE);
  }
''' + s[b:]
a=s.index('    if (sprite == null)',s.index('  public void drawSprite('));b=s.index('\n  public void drawItem(',a)
s=s[:a]+'''    if (sprite == null) {
      sprite = ((TextureAtlas) this.minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS))
          .getSprite(MissingTextureAtlasSprite.getLocation());
    }
    x += this.leftPos;
    y += this.topPos;
    double tileSize = scale * 16.0;
    if (tileSize <= 0 || width <= 0 || height <= 0) return;
    this.guiGraphics.enableScissor((int) x, (int) y, (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    double startX = fixRight ? x + width - Math.ceil(width / tileSize) * tileSize : x;
    double startY = fixBottom ? y + height - Math.ceil(height / tileSize) * tileSize : y;
    for (double tileX = startX; tileX < x + width; tileX += tileSize) {
      for (double tileY = startY; tileY < y + height; tileY += tileSize) {
        this.guiGraphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            sprite, (int) tileX, (int) tileY, (int) Math.ceil(tileSize), (int) Math.ceil(tileSize), color);
      }
    }
    this.guiGraphics.disableScissor();
  }
''' + s[b:]
a=s.index('    x += this.leftPos;',s.index('  public void drawColoredRect('));b=s.index('\n  public int drawString(',a)
s=s[:a]+'''    x += this.leftPos;
    y += this.topPos;
    this.guiGraphics.fill(x, y, x + width, y + height, color);
  }
''' + s[b:]
s=s.replace('guiGraphics.renderTooltip(', 'guiGraphics.setTooltipForNextFrame(')
s=s.replace('InventoryMenu.BLOCK_ATLAS','TextureAtlas.LOCATION_BLOCKS')
# Convert entry points to new event records while preserving existing subclass handlers.
s=s.replace('return super.mouseClicked(mouseX, mouseY, mouseButton);','return super.mouseClicked(mouseEvent(mouseX, mouseY, mouseButton), false);')
s=s.replace('return super.mouseDragged(mouseX, mouseY, clickedMouseButton, deltaX, deltaY);','return super.mouseDragged(mouseEvent(mouseX, mouseY, clickedMouseButton), deltaX, deltaY);')
s=s.replace('return super.mouseReleased(mouseX, mouseY, state);','return super.mouseReleased(mouseEvent(mouseX, mouseY, state));')
s=s.replace('return super.charTyped(typedChar, keyCode);','return super.charTyped(new net.minecraft.client.input.CharacterEvent(typedChar));')
a=s.index('  public boolean mouseClicked(double mouseX')
s=s[:a]+'''  private static net.minecraft.client.input.MouseButtonEvent mouseEvent(double x, double y, int button) {
    int sdlButton = button == 0 ? 1 : button == 1 ? 3 : button + 1;
    return new net.minecraft.client.input.MouseButtonEvent(x, y,
        new net.minecraft.client.input.MouseButtonInfo(sdlButton, 0));
  }

  private static int legacyButton(int button) { return button == 1 ? 0 : button == 3 ? 1 : 2; }

  @Override
  public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
    return this.mouseClicked(event.x(), event.y(), legacyButton(event.button()));
  }

  @Override
  public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
    return this.mouseDragged(event.x(), event.y(), legacyButton(event.button()), dx, dy);
  }

  @Override
  public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
    return this.mouseReleased(event.x(), event.y(), legacyButton(event.button()));
  }

  @Override
  public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
    boolean handled = false;
    for (char character : event.codepointAsString().toCharArray()) handled |= charTyped(character, 0);
    return handled;
  }

'''+s[a:]
p.write_text(s)
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();orig=s
 if 'GuiGraphicsExtractor' in s:
  s=re.sub(r'\b((?:this\.)?(?:guiGraphics|graphics))\.drawString\(',r'ic2.compat.GuiCompat.drawString(\1, ',s)
  s=s.replace('guiGraphics.renderTooltip(', 'guiGraphics.setTooltipForNextFrame(')
 if s!=orig:p.write_text(s)
