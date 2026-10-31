package twilightforest.client.overlay.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public abstract class SimpleTextDisplay implements ItemDisplay {

	public abstract Component getText(ItemStack item, Minecraft minecraft, Hud gui, Player player);

	@Override
	public void render(ItemStack item, GuiGraphicsExtractor graphics, Minecraft minecraft, Hud gui, Player player, int widestWidgetWidth) {
		graphics.centeredText(minecraft.font, this.getText(item, minecraft, gui, player), widestWidgetWidth / 2, 0, 0xFFFFFFFF);
	}

	@Override
	public Bounds getWidgetSize(ItemStack item, Minecraft minecraft, Hud gui, Player player, int widestWidgetWidth) {
		int textWidth = minecraft.font.width(this.getText(item, minecraft, gui, player));
		return new Bounds(Math.max(0, (widestWidgetWidth / 2) - (textWidth / 2)), 0, textWidth, minecraft.font.lineHeight);
	}
}
