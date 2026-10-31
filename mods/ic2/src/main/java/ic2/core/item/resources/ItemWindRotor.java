package ic2.core.item.resources;

import ic2.api.item.IKineticRotor;
import ic2.core.block.kineticgenerator.gui.GuiWaterKineticGenerator;
import ic2.core.block.kineticgenerator.gui.GuiWindKineticGenerator;
import ic2.core.profile.NotClassic;
import ic2.core.util.Ic2Tooltip;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@NotClassic
public class ItemWindRotor extends Item implements IKineticRotor {
  private final int maxWindStrength;
  private final int minWindStrength;
  private final int radius;
  private final float efficiency;
  private final Identifier renderTexture;
  private final boolean water;

  public ItemWindRotor(
      Properties settings,
      int Radius,
      boolean water,
      float efficiency,
      int minWindStrength,
      int maxWindStrength,
      Identifier RenderTexture) {
    super(settings);
    this.radius = Radius;
    this.efficiency = efficiency;
    this.renderTexture = RenderTexture;
    this.minWindStrength = minWindStrength;
    this.maxWindStrength = maxWindStrength;
    this.water = water;
  }

  @OnlyIn(Dist.CLIENT)
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
      ItemStack stack, Item.TooltipContext world, List<Component> tooltip, TooltipFlag advanced) {
    Ic2Tooltip.add(
        tooltip,
        Component.translatable(
            "ic2.itemrotor.wind.info", this.minWindStrength, this.maxWindStrength));
    IKineticRotor.GearboxType type = null;
    if (Minecraft.getInstance().gui.screen() instanceof GuiWaterKineticGenerator) {
      type = IKineticRotor.GearboxType.WATER;
    } else if (Minecraft.getInstance().gui.screen() instanceof GuiWindKineticGenerator) {
      type = IKineticRotor.GearboxType.WIND;
    }

    if (type != null) {
      Ic2Tooltip.add(
          tooltip,
          Component.translatable("ic2.itemrotor.fitsin." + this.isAcceptedType(stack, type)));
    }
  }

  @Override
  public int getDiameter(ItemStack stack) {
    return this.radius;
  }

  @Override
  public Identifier getRotorRenderTexture(ItemStack stack) {
    return this.renderTexture;
  }

  @Override
  public float getEfficiency(ItemStack stack) {
    return this.efficiency;
  }

  @Override
  public int getMinWindStrength(ItemStack stack) {
    return this.minWindStrength;
  }

  @Override
  public int getMaxWindStrength(ItemStack stack) {
    return this.maxWindStrength;
  }

  @Override
  public boolean isAcceptedType(ItemStack stack, IKineticRotor.GearboxType type) {
    return type == IKineticRotor.GearboxType.WIND || this.water;
  }
}
