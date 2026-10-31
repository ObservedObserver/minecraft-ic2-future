package ic2.core.item.reactor;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;
import ic2.core.util.Ic2Tooltip;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDamageableReactorComponent extends Item implements IReactorComponent {
  private final int maxUse;

  protected AbstractDamageableReactorComponent(Properties settings, int maxUse) {
    super(settings);
    this.maxUse = maxUse;
  }

  @Override
  public void processChamber(ItemStack stack, IReactor reactor, int x, int y, boolean heatRun) {}

  @Override
  public boolean acceptUraniumPulse(
      ItemStack stack,
      IReactor reactor,
      ItemStack pulsingStack,
      int youX,
      int youY,
      int pulseX,
      int pulseY,
      boolean heatrun) {
    return false;
  }

  @Override
  public boolean canStoreHeat(ItemStack stack, IReactor reactor, int x, int y) {
    return false;
  }

  @Override
  public int getMaxHeat(ItemStack stack, IReactor reactor, int x, int y) {
    return 0;
  }

  @Override
  public int getCurrentHeat(ItemStack stack, IReactor reactor, int x, int y) {
    return 0;
  }

  @Override
  public int alterHeat(ItemStack stack, IReactor reactor, int x, int y, int heat) {
    return heat;
  }

  @Override
  public float influenceExplosion(ItemStack stack, IReactor reactor) {
    return 0.0F;
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
    super.appendHoverText(
        stack,
        world,
        net.minecraft.world.item.component.TooltipDisplay.DEFAULT,
        tooltip::add,
        advanced);
    Ic2Tooltip.add(
        tooltip,
        Component.translatable(
            "ic2.reactoritem.durability", this.getMaxUse() - this.getUse(stack), this.getMaxUse()));
  }

  @Override
  public boolean canBePlacedIn(ItemStack stack, IReactor reactor) {
    return true;
  }

  public int getUse(ItemStack stack) {
    CompoundTag nbt = StackUtil.getTag(stack);
    return nbt != null ? nbt.getIntOr("use", 0) : 0;
  }

  public void setUse(ItemStack stack, int use) {
    StackUtil.setTagValue(stack, "use", net.minecraft.nbt.IntTag.valueOf(use));
  }

  protected void incrementUse(ItemStack stack) {
    StackUtil.setTagValue(
        stack,
        "use",
        net.minecraft.nbt.IntTag.valueOf(Math.min(this.getUse(stack) + 1, this.maxUse)));
  }

  public int getMaxUse() {
    return this.maxUse;
  }

  public double getUseFraction(ItemStack stack) {
    return Util.limit((double) this.getUse(stack) / this.maxUse, 0.0, 1.0);
  }

  public boolean isBarVisible(@NotNull ItemStack stack) {
    return true;
  }

  public int getBarWidth(@NotNull ItemStack stack) {
    return (int) Math.round((1.0 - this.getUseFraction(stack)) * 13.0);
  }

  public int getBarColor(@NotNull ItemStack stack) {
    return Mth.hsvToRgb((float) ((1.0 - this.getUseFraction(stack)) / 3.0), 1.0F, 1.0F);
  }
}
