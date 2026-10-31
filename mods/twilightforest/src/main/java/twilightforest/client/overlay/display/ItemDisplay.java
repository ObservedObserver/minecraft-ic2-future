package twilightforest.client.overlay.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ItemDisplay {

	void render(ItemStack item, GuiGraphicsExtractor graphics, Minecraft minecraft, Hud gui, Player player, int widestWidgetWidth);

	default DisplayPosition displayPosition() {
		return DisplayPosition.DEFAULT;
	}

	Bounds getWidgetSize(ItemStack item, Minecraft minecraft, Hud gui, Player player, int widestWidgetWidth);

	enum DisplayPosition {
		TOP,
		DEFAULT,
		BOTTOM
	}

	record Bounds(int startX, int startY, int width, int height) {

	}
}
