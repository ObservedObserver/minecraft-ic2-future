package ic2.core.item.tool;

import ic2.core.Ic2Gui;
import ic2.core.util.Tuple;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GuiToolScanner extends Ic2Gui<ContainerToolScanner> {
  public GuiToolScanner(
      ContainerToolScanner container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title, 230);
  }

  @Override
  protected void drawForegroundLayer(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
    super.drawForegroundLayer(guiGraphics, mouseX, mouseY);
    this.drawString(
        guiGraphics, 10, 20, Component.translatable("ic2.scanner.found").getString(), 2157374);
    if (this.menu.scanResults != null) {
      int count = 0;

      for (Tuple.T2<ItemStack, Integer> result : this.menu.scanResults) {
        String name = Component.translatable(result.a.getItem().getDescriptionId()).getString();
        this.drawString(guiGraphics, 10, 34 + count * 11, result.b + "x " + name, 5752026);
        if (++count == 10) {
          break;
        }
      }
    }
  }

  @Override
  public void extractBackground(
      GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float delta) {
    super.extractBackground(guiGraphics, mouseX, mouseY, delta);
    if (this.menu.scanResults != null) {
      int count = 0;

      for (Tuple.T2<ItemStack, Integer> result : this.menu.scanResults) {
        int xPos = 135 + (count & 1) * 15;
        this.drawItem(xPos, 11 * count + 28, result.a);
        if (++count == 10) {
          break;
        }
      }
    }
  }

  @Override
  public Identifier getTextureLocation() {
    return Identifier.fromNamespaceAndPath("ic2", "textures/gui/guitoolscanner.png");
  }
}
