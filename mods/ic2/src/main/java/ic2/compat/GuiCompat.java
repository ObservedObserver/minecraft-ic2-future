package ic2.compat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class GuiCompat {
  private GuiCompat() {}

  public static int drawString(
      GuiGraphicsExtractor graphics,
      Font font,
      String text,
      int x,
      int y,
      int color,
      boolean shadow) {
    graphics.text(font, text, x, y, opaque(color), shadow);
    return x + font.width(text);
  }

  public static int drawString(
      GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
    return drawString(graphics, font, text, x, y, color, true);
  }

  public static int drawString(
      GuiGraphicsExtractor graphics,
      Font font,
      Component text,
      int x,
      int y,
      int color,
      boolean shadow) {
    graphics.text(font, text, x, y, opaque(color), shadow);
    return x + font.width(text);
  }

  public static int drawString(
      GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
    return drawString(graphics, font, text, x, y, color, true);
  }

  private static int opaque(int color) {
    return (color & 0xFF000000) == 0 ? color | 0xFF000000 : color;
  }
}
