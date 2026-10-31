package ic2.core.item;

import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.block.tileentity.Ic2TileEntityBlock;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class ItemBlockIc2 extends BlockItem {
  public ItemBlockIc2(Block block, Properties properties) {
    super(block, properties.useBlockDescriptionPrefix());
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
      @NotNull ItemStack stack,
      Item.TooltipContext level,
      @NotNull List<Component> tooltip,
      @NotNull TooltipFlag flag) {
    if (this.getBlock() instanceof Ic2TileEntityBlock block) {
      Ic2TileEntity dummyTe = block.getDummyTe();
      dummyTe.appendItemTooltip(stack, tooltip, flag);
    }
  }
}
