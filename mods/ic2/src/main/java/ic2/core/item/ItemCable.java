package ic2.core.item;

import ic2.core.block.wiring.AbstractCableBlock;
import ic2.core.util.Ic2Tooltip;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ItemCable extends BlockItem {
  private final AbstractCableBlock cableBlock;

  public ItemCable(Block block, Properties p) {
    super(block, p.useBlockDescriptionPrefix());
    this.cableBlock = (AbstractCableBlock) block;
  }

  @Override
  public void appendHoverText(
      net.minecraft.world.item.ItemStack stack,
      net.minecraft.world.item.Item.TooltipContext context,
      net.minecraft.world.item.component.TooltipDisplay display,
      java.util.function.Consumer<net.minecraft.network.chat.Component> output,
      net.minecraft.world.item.TooltipFlag flag) {
    if (display.hideTooltip()) return;
    java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
    this.appendHoverText(stack, context, lines, flag);
    lines.forEach(output);
  }

  public void appendHoverText(
      @NotNull ItemStack item,
      Item.TooltipContext level,
      @NotNull List<Component> component,
      @NotNull TooltipFlag flag) {
    Ic2Tooltip.add(
        component, Component.translatable("item.ic2.cable.tooltip0", cableBlock.type.capacity));
    Ic2Tooltip.add(
        component, Component.translatable("item.ic2.cable.tooltip1", cableBlock.getLoss()));
  }
}
