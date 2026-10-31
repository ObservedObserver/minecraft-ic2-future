package ic2.core;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.api.upgrade.UpgradeRegistry;
import ic2.core.gui.GuiElement;
import ic2.core.gui.IClickHandler;
import ic2.core.gui.IKeyboardDependent;
import ic2.core.gui.MouseButton;
import ic2.core.gui.ScrollDirection;
import ic2.core.util.StackUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class Ic2Gui<T extends ContainerBase<? extends Container>>
    extends AbstractContainerScreen<T> {
  public static final int textHeight = 8;
  protected static Runnable closeHandler;
  protected final List<GuiElement<?>> elements = new ArrayList<>();
  private final Set<GuiElement.ImplementedMethod> elementMethods =
      EnumSet.noneOf(GuiElement.ImplementedMethod.class);
  private final Queue<Ic2Gui.Tooltip> queuedTooltips = new ArrayDeque<>();
  private boolean fixKeyEvents = false;
  private GuiGraphicsExtractor guiGraphics;
  private static Identifier boundTexture;
  private net.minecraft.client.input.MouseButtonEvent originalMouseEvent;
  private boolean originalDoubleClick;

  public Ic2Gui(T container, Inventory playerInventory, Component title) {
    this(container, playerInventory, title, 176, 166);
  }

  public Ic2Gui(T container, Inventory playerInventory, Component title, int ySize) {
    this(container, playerInventory, title, 176, ySize);
  }

  public Ic2Gui(T container, Inventory playerInventory, Component title, int xSize, int ySize) {
    super(container, playerInventory, title, xSize, ySize);
  }

  private static List<ItemStack> getCompatibleUpgrades(IUpgradableBlock block) {
    List<ItemStack> ret = new ArrayList<>();
    Set<UpgradableProperty> properties = block.getUpgradableProperties();

    for (ItemStack stack : UpgradeRegistry.getUpgrades()) {
      IUpgradeItem item = (IUpgradeItem) stack.getItem();
      if (item.isSuitableFor(stack, properties)) {
        ret.add(stack);
      }
    }

    return ret;
  }

  public static void bindTexture(Identifier id) {
    boundTexture = id;
  }

  public final T getContainer() {
    return this.menu;
  }

  public final int getX() {
    return this.leftPos;
  }

  public final int getY() {
    return this.topPos;
  }

  public final Slot getFocusedSlot() {
    return this.hoveredSlot;
  }

  public void init() {
    super.init();

    for (GuiElement<?> element : this.elements) {
      if (element instanceof IKeyboardDependent) {
        // TODO: In 1.20.1, keyboardHandler.setSendRepeatsToGui was removed.
        // this.minecraft.keyboardHandler.setSendRepeatsToGui(true);
        this.fixKeyEvents = true;
        break;
      }
    }
  }

  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
    this.guiGraphics = guiGraphics;
    super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
  }

  public void containerTick() {
    super.containerTick();
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.tick)) {
      for (GuiElement<?> element : this.elements) {
        if (element.isEnabled()) {
          element.tick();
        }
      }
    }
  }

  public void extractBackground(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
    this.guiGraphics = guiGraphics;
    super.extractBackground(guiGraphics, mouseX, mouseY, delta);
    mouseX -= this.leftPos;
    mouseY -= this.topPos;
    this.drawBackgroundAndTitle(guiGraphics, delta, mouseX, mouseY);
    if (this.menu.base instanceof IUpgradableBlock) {
      bindTexture(Identifier.fromNamespaceAndPath("ic2", "textures/gui/infobutton.png"));
      this.drawTexturedRect(guiGraphics.pose(), 3.0, 3.0, 10.0, 10.0, 0.0, 0.0);
    }

    if (this.elementMethods.contains(GuiElement.ImplementedMethod.drawBackground)) {
      for (GuiElement<?> element : this.elements) {
        if (element.isEnabled()) {
          element.drawBackground(guiGraphics, mouseX, mouseY);
        }
      }
    }
  }

  protected void drawBackgroundAndTitle(
      GuiGraphicsExtractor guiGraphics, float partialTicks, int mouseX, int mouseY) {
    this.bindTexture();
    guiGraphics.blit(
        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        this.getTextureLocation(),
        this.leftPos,
        this.topPos,
        0.0F,
        0.0F,
        this.imageWidth,
        this.imageHeight,
        256,
        256);
    this.drawXCenteredString(guiGraphics, this.imageWidth / 2, 6, this.title, 4210752, false);
  }

  protected final void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    guiGraphics.pose().pushMatrix();
    guiGraphics.pose().translate(-this.leftPos, -this.topPos);
    try {
      this.drawForegroundLayer(guiGraphics, mouseX - this.leftPos, mouseY - this.topPos);
      this.flushTooltips(guiGraphics);
    } finally {
      guiGraphics.pose().popMatrix();
    }
  }

  protected void drawForegroundLayer(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    if (this.menu.base instanceof IUpgradableBlock) {
      this.handleUpgradeTooltip(mouseX, mouseY);
    }

    for (GuiElement<?> element : this.elements) {
      if (element.isEnabled()) {
        element.drawForeground(guiGraphics, mouseX, mouseY);
      }
    }
  }

  private void handleUpgradeTooltip(int mouseX, int mouseY) {
    int areaSize = 12;
    if (mouseX >= 0 && mouseX <= 12 && mouseY >= 0 && mouseY <= 12) {
      List<Component> text = new ArrayList<>();
      text.add(Component.translatable("ic2.generic.text.upgrade"));

      for (ItemStack stack : getCompatibleUpgrades((IUpgradableBlock) this.menu.base)) {
        text.add(stack.getHoverName().copy().withStyle(ChatFormatting.GRAY));
      }

      this.drawTooltip(mouseX, mouseY, text);
    }
  }

  public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta, double scrollY) {
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.onMouseScroll)) {
      ScrollDirection direction;
      if (scrollY != 0.0) {
        direction = scrollY < 0.0 ? ScrollDirection.down : ScrollDirection.up;
      } else {
        direction = ScrollDirection.stopped;
      }

      for (GuiElement<?> element : this.elements) {
        if (element.isEnabled()
            && element.contains((int) mouseX - this.leftPos, (int) mouseY - this.topPos)) {
          element.onMouseScroll((int) mouseX - this.leftPos, (int) mouseY - this.topPos, direction);
        }
      }
    }

    return super.mouseScrolled(mouseX, mouseY, scrollDelta, scrollY);
  }

  private net.minecraft.client.input.MouseButtonEvent mouseEvent(double x, double y, int button) {
    if (this.originalMouseEvent != null) return this.originalMouseEvent;
    int sdlButton = button == 0 ? 1 : button == 1 ? 3 : button == 2 ? 2 : button + 1;
    return new net.minecraft.client.input.MouseButtonEvent(
        x, y, new net.minecraft.client.input.MouseButtonInfo(sdlButton, 0));
  }

  private static int legacyButton(int button) {
    return button == 1 ? 0 : button == 3 ? 1 : button == 2 ? 2 : button - 1;
  }

  @Override
  public boolean mouseClicked(
      net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
    var previous = this.originalMouseEvent;
    boolean previousDoubleClick = this.originalDoubleClick;
    this.originalMouseEvent = event;
    this.originalDoubleClick = doubleClick;
    try {
      return this.mouseClicked(event.x(), event.y(), legacyButton(event.button()));
    } finally {
      this.originalMouseEvent = previous;
      this.originalDoubleClick = previousDoubleClick;
    }
  }

  @Override
  public boolean mouseDragged(
      net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
    var previous = this.originalMouseEvent;
    boolean previousDoubleClick = this.originalDoubleClick;
    this.originalMouseEvent = event;
    try {
      return this.mouseDragged(event.x(), event.y(), legacyButton(event.button()), dx, dy);
    } finally {
      this.originalMouseEvent = previous;
      this.originalDoubleClick = previousDoubleClick;
    }
  }

  @Override
  public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
    var previous = this.originalMouseEvent;
    boolean previousDoubleClick = this.originalDoubleClick;
    this.originalMouseEvent = event;
    try {
      return this.mouseReleased(event.x(), event.y(), legacyButton(event.button()));
    } finally {
      this.originalMouseEvent = previous;
      this.originalDoubleClick = previousDoubleClick;
    }
  }

  @Override
  public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.onKeyTyped)) {
      for (GuiElement<?> element : this.elements) {
        if (element.isEnabled() && element.onKeyTyped('\0', event.key())) return true;
      }
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
    boolean handled = false;
    for (char character : event.codepointAsString().toCharArray())
      handled |= charTyped(character, 0);
    return handled;
  }

  public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.onMouseClick)) {
      MouseButton button = MouseButton.get(mouseButton);
      if (button != null) {
        boolean handled = false;
        mouseX -= this.leftPos;
        mouseY -= this.topPos;

        for (GuiElement<?> element : this.elements) {
          if (element.isEnabled()) {
            handled |=
                element.onMouseClick(
                    (int) mouseX,
                    (int) mouseY,
                    button,
                    element.contains((int) mouseX, (int) mouseY));
          }
        }

        if (handled) {
          return true;
        }

        mouseX += this.leftPos;
        mouseY += this.topPos;
      }
    }

    return super.mouseClicked(mouseEvent(mouseX, mouseY, mouseButton), this.originalDoubleClick);
  }

  public boolean mouseDragged(
      double mouseX, double mouseY, int clickedMouseButton, double deltaX, double deltaY) {
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.onMouseDrag)) {
      MouseButton button = MouseButton.get(clickedMouseButton);
      if (button != null) {
        boolean handled = false;
        mouseX -= this.leftPos;
        mouseY -= this.topPos;

        for (GuiElement<?> element : this.elements) {
          if (element.isEnabled()) {
            handled |=
                element.onMouseDrag(
                    (int) mouseX,
                    (int) mouseY,
                    button,
                    element.contains((int) mouseX, (int) mouseY));
          }
        }

        if (handled) {
          return true;
        }

        mouseX += this.leftPos;
        mouseY += this.topPos;
      }
    }

    return super.mouseDragged(mouseEvent(mouseX, mouseY, clickedMouseButton), deltaX, deltaY);
  }

  public boolean mouseReleased(double mouseX, double mouseY, int state) {
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.onMouseRelease)) {
      MouseButton button = MouseButton.get(state);
      if (button != null) {
        boolean handled = false;
        mouseX -= this.leftPos;
        mouseY -= this.topPos;

        for (GuiElement<?> element : this.elements) {
          if (element.isEnabled()) {
            handled |=
                element.onMouseRelease(
                    (int) mouseX,
                    (int) mouseY,
                    button,
                    element.contains((int) mouseX, (int) mouseY));
          }
        }

        if (handled) {
          return true;
        }

        mouseX += this.leftPos;
        mouseY += this.topPos;
      }
    }

    return super.mouseReleased(mouseEvent(mouseX, mouseY, state));
  }

  public boolean charTyped(char typedChar, int keyCode) {
    if (this.elementMethods.contains(GuiElement.ImplementedMethod.onKeyTyped)) {
      boolean handled = false;

      for (GuiElement<?> element : this.elements) {
        if (element.isEnabled()) {
          handled |= element.onKeyTyped(typedChar, keyCode);
        }
      }

      if (handled) {
        return true;
      }
    }

    return super.charTyped(new net.minecraft.client.input.CharacterEvent(typedChar));
  }

  public void removed() {
    super.removed();
    // TODO: In 1.20.1, keyboardHandler.setSendRepeatsToGui was removed.
    // this.minecraft.keyboardHandler.setSendRepeatsToGui(false);

    if (closeHandler != null) {
      closeHandler.run();
    }
  }

  public void drawTexturedRect(
      org.joml.Matrix3x2fStack matrices,
      double x,
      double y,
      double width,
      double height,
      double texX,
      double texY) {
    this.drawTexturedRect(matrices, x, y, width, height, texX, texY, false);
  }

  public void drawTexturedRect(
      org.joml.Matrix3x2fStack matrices,
      double x,
      double y,
      double width,
      double height,
      double texX,
      double texY,
      boolean mirrorX) {
    this.drawTexturedRect(
        matrices,
        x,
        y,
        width,
        height,
        texX / 256.0,
        texY / 256.0,
        (texX + width) / 256.0,
        (texY + height) / 256.0,
        mirrorX);
  }

  public void drawTexturedRect(
      org.joml.Matrix3x2fStack matrices,
      double x,
      double y,
      double width,
      double height,
      double uS,
      double vS,
      double uE,
      double vE,
      boolean mirrorX) {
    x += this.leftPos;
    y += this.topPos;
    double xE = x + width;
    double yE = y + height;
    if (mirrorX) {
      double tmp = uS;
      uS = uE;
      uE = tmp;
    }

    this.guiGraphics.blit(
        boundTexture,
        (int) x,
        (int) y,
        (int) xE,
        (int) yE,
        (float) uS,
        (float) uE,
        (float) vS,
        (float) vE);
  }

  public void drawSprite(
      org.joml.Matrix3x2fStack matrices,
      double x,
      double y,
      double width,
      double height,
      TextureAtlasSprite sprite,
      int color,
      double scale,
      boolean fixRight,
      boolean fixBottom) {
    if (sprite == null) {
      sprite =
          ((TextureAtlas)
                  this.minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS))
              .getSprite(MissingTextureAtlasSprite.getLocation());
    }
    x += this.leftPos;
    y += this.topPos;
    double tileSize = scale * 16.0;
    if (tileSize <= 0 || width <= 0 || height <= 0) return;
    this.guiGraphics.enableScissor(
        (int) x, (int) y, (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    double startX = fixRight ? x + width - Math.ceil(width / tileSize) * tileSize : x;
    double startY = fixBottom ? y + height - Math.ceil(height / tileSize) * tileSize : y;
    for (double tileX = startX; tileX < x + width; tileX += tileSize) {
      for (double tileY = startY; tileY < y + height; tileY += tileSize) {
        this.guiGraphics.blitSprite(
            net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            sprite,
            (int) tileX,
            (int) tileY,
            (int) Math.ceil(tileSize),
            (int) Math.ceil(tileSize),
            color);
      }
    }
    this.guiGraphics.disableScissor();
  }

  public void drawItem(int x, int y, ItemStack stack) {
    if (this.guiGraphics != null) {
      this.guiGraphics.item(stack, this.leftPos + x, this.topPos + y);
    }
  }

  public void drawItemStack(int x, int y, ItemStack stack) {
    this.drawItem(x, y, stack);
    if (this.guiGraphics != null) {
      this.guiGraphics.itemDecorations(this.font, stack, this.leftPos + x, this.topPos + y, null);
    }
  }

  public void drawColoredRect(
      org.joml.Matrix3x2fStack matrices, int x, int y, int width, int height, int color) {
    x += this.leftPos;
    y += this.topPos;
    this.guiGraphics.fill(x, y, x + width, y + height, color);
  }

  public int drawString(GuiGraphicsExtractor guiGraphics, int x, int y, String text, int color) {
    return ic2.compat.GuiCompat.drawString(
            guiGraphics, this.font, text, this.leftPos + x, this.topPos + y, color, false)
        - this.leftPos;
  }

  public int drawString(
      GuiGraphicsExtractor guiGraphics, int x, int y, String text, int color, boolean ignored) {
    return ic2.compat.GuiCompat.drawString(
            guiGraphics, this.font, text, this.leftPos + x, this.topPos + y, color, false)
        - this.leftPos;
  }

  public void drawTrimmedString(
      GuiGraphicsExtractor guiGraphics, int x, int y, String text, int maxWidth, int color) {
    int lineY = this.topPos + y;
    for (var line : this.font.split(Component.literal(text), maxWidth)) {
      guiGraphics.text(this.font, line, this.leftPos + x, lineY, color, false);
      lineY += 10;
    }
  }

  public void drawXCenteredString(
      GuiGraphicsExtractor guiGraphics, int x, int y, String text, int color, boolean shadow) {
    this.drawCenteredString(guiGraphics, x, y, text, color, shadow, true, false);
  }

  public void drawXCenteredString(
      GuiGraphicsExtractor guiGraphics, int x, int y, Component text, int color, boolean shadow) {
    this.drawCenteredString(guiGraphics, x, y, text, color, shadow, true, false);
  }

  public void drawXYCenteredString(
      GuiGraphicsExtractor guiGraphics, int x, int y, String text, int color, boolean shadow) {
    this.drawCenteredString(guiGraphics, x, y, text, color, shadow, true, true);
  }

  public void drawXYCenteredString(
      GuiGraphicsExtractor guiGraphics, int x, int y, Component text, int color, boolean shadow) {
    this.drawCenteredString(guiGraphics, x, y, text, color, shadow, true, true);
  }

  public void drawCenteredString(
      GuiGraphicsExtractor guiGraphics,
      int x,
      int y,
      String text,
      int color,
      boolean shadow,
      boolean centerX,
      boolean centerY) {
    if (centerX) {
      x -= this.getStringWidth(text) / 2;
    }

    if (centerY) {
      y -= 4;
    }

    ic2.compat.GuiCompat.drawString(
        guiGraphics, this.font, text, this.leftPos + x, this.topPos + y, color, shadow);
  }

  public void drawCenteredString(
      GuiGraphicsExtractor guiGraphics,
      int x,
      int y,
      Component text,
      int color,
      boolean shadow,
      boolean centerX,
      boolean centerY) {
    if (centerX) {
      x -= this.getStringWidth(text) / 2;
    }

    if (centerY) {
      y -= 4;
    }

    ic2.compat.GuiCompat.drawString(
        guiGraphics, this.font, text, this.leftPos + x, this.topPos + y, color, shadow);
  }

  public int getStringWidth(String text) {
    return this.font.width(text);
  }

  public int getStringWidth(Component text) {
    return this.font.width(text);
  }

  public String trimStringToWidth(String text, int width) {
    return this.font.plainSubstrByWidth(text, width, false);
  }

  public String trimStringToWidthReverse(String text, int width) {
    return this.font.plainSubstrByWidth(text, width, true);
  }

  public void drawTooltip(int x, int y, List<Component> text) {
    // 清空之前的 tooltip，确保只有最后处理的（视觉最上层的）元素 tooltip 被渲染
    // 避免 TankGauge + SlotGrid 等重叠元素的 tooltip 叠加显示
    this.queuedTooltips.clear();
    this.queuedTooltips.add(new Ic2Gui.Tooltip(text, x, y));
  }

  public void drawTooltip(GuiGraphicsExtractor guiGraphics, int x, int y, ItemStack stack) {
    assert !StackUtil.isEmpty(stack);
    guiGraphics.setTooltipForNextFrame(this.font, stack, this.leftPos + x, this.topPos + y);
  }

  protected void flushTooltips(GuiGraphicsExtractor guiGraphics) {
    // 如果 vanilla 接下来会渲染 slot 物品 tooltip，跳过 IC2 的 tooltip，避免与物品 tooltip 重叠
    if (this.hoveredSlot != null
        && this.hoveredSlot.hasItem()
        && this.menu.getCarried().isEmpty()) {
      this.queuedTooltips.clear();
      return;
    }

    for (Ic2Gui.Tooltip tooltip : this.queuedTooltips) {
      guiGraphics.setTooltipForNextFrame(
          this.font,
          tooltip.text,
          java.util.Optional.empty(),
          this.leftPos + tooltip.x,
          this.topPos + tooltip.y);
    }

    this.queuedTooltips.clear();
  }

  protected void addElement(GuiElement<?> element) {
    this.elements.add(element);
    this.elementMethods.addAll(element.getImplementedMethods());
  }

  protected final void bindTexture() {
    bindTexture(this.getTextureLocation());
  }

  protected IClickHandler createEventSender(int event) {
    if (this.menu.base instanceof BlockEntity) {
      return button ->
          IC2.network
              .get(false)
              .initiateClientTileEntityEvent((BlockEntity) Ic2Gui.this.menu.base, event);
    } else {
      throw new IllegalArgumentException(
          "not applicable for " + ((ContainerBase<?>) this.menu).base);
    }
  }

  protected abstract Identifier getTextureLocation();

  private record Tooltip(List<Component> text, int x, int y) {}
}
