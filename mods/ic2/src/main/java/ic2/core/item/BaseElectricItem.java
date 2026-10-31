package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public abstract class BaseElectricItem extends Item implements IElectricItem, IItemHudInfo {
  protected final double maxCharge;
  protected final double transferLimit;
  protected final int tier;

  public BaseElectricItem(Properties settings, double maxCharge, double transferLimit, int tier) {
    super(settings);
    this.maxCharge = maxCharge;
    this.transferLimit = transferLimit;
    this.tier = tier;
  }

  @Override
  public boolean canProvideEnergy(ItemStack stack) {
    return false;
  }

  @Override
  public double getMaxCharge(ItemStack stack) {
    return this.maxCharge;
  }

  @Override
  public int getTier(ItemStack stack) {
    return this.tier;
  }

  @Override
  public double getTransferLimit(ItemStack stack) {
    return this.transferLimit;
  }

  @Override
  public List<String> getHudInfo(ItemStack stack, boolean advanced) {
    List<String> info = new LinkedList<>();
    info.add(ElectricItem.manager.getToolTip(stack));
    return info;
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
      @NotNull TooltipFlag context) {
    ElectricItemTooltipHandler.addTooltip(stack, tooltip);
  }

  public boolean isBarVisible(@NotNull ItemStack stack) {
    return ElectricItem.manager.getCharge(stack) <= ElectricItem.manager.getMaxCharge(stack);
  }

  public int getBarWidth(@NotNull ItemStack stack) {
    return (int) Math.round(ElectricItem.manager.getStackChargeLevel(stack) * 13.0);
  }

  public int getBarColor(@NotNull ItemStack stack) {
    return Mth.hsvToRgb(
        (float) (ElectricItem.manager.getStackChargeLevel(stack) / 3.0), 1.0F, 1.0F);
  }
}
