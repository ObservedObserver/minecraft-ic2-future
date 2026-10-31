package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.core.util.Ic2Tooltip;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public class ItemReactorHeatStorage extends AbstractDamageableReactorComponent {
  public ItemReactorHeatStorage(Properties settings, int heatStorage) {
    super(settings, heatStorage);
  }

  @Override
  public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
    return true;
  }

  @Override
  public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
    return this.getMaxUse();
  }

  @Override
  public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
    return this.getUse(stack);
  }

  @Override
  public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
    int myHeat = this.getCurrentHeat(stack, reactor, x, y);
    myHeat += heat;
    int max = this.getMaxHeat(stack, reactor, x, y);
    if (myHeat > max) {
      reactor.setItemAt(x, y, null);
      heat = max - myHeat + 1;
    } else {
      if (myHeat < 0) {
        heat = myHeat;
        myHeat = 0;
      } else {
        heat = 0;
      }

      this.setUse(stack, myHeat);
    }

    return heat;
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
      Item.TooltipContext world,
      @NotNull List<Component> tooltip,
      @NotNull TooltipFlag advanced) {
    super.appendHoverText(stack, world, tooltip, advanced);
    if (this.getUse(stack) > 0) {
      Ic2Tooltip.add(tooltip, Component.translatable("ic2.reactoritem.heatwarning.line1"));
      Ic2Tooltip.add(tooltip, Component.translatable("ic2.reactoritem.heatwarning.line2"));
    }
  }
}
