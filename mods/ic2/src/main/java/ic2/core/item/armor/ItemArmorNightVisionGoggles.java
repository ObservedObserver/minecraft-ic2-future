package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import ic2.core.item.ElectricItemTooltipHandler;
import ic2.core.ref.Ic2ArmorMaterials;
import ic2.core.util.Ic2Tooltip;
import ic2.core.util.KeyboardClient;
import ic2.core.util.StackUtil;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemArmorNightVisionGoggles extends ItemArmorUtility
    implements IElectricItem, IItemHudInfo, ic2.compat.LegacyInventoryTicker {
  public ItemArmorNightVisionGoggles(Properties settings) {
    super(Ic2ArmorMaterials.NIGHT_VISION_GOGGLES.holder(), settings, EquipmentSlot.HEAD);
  }

  @Override
  public boolean canProvideEnergy(ItemStack stack) {
    return false;
  }

  @Override
  public double getMaxCharge(ItemStack stack) {
    return 200000.0;
  }

  @Override
  public int getTier(ItemStack stack) {
    return 1;
  }

  @Override
  public double getTransferLimit(ItemStack stack) {
    return 200.0;
  }

  @Override
  public List<String> getHudInfo(ItemStack stack, boolean advanced) {
    List<String> info = new LinkedList<>();
    info.add(ElectricItem.manager.getToolTip(stack));
    return info;
  }

  @Override
  public void inventoryTick(
      net.minecraft.world.item.ItemStack stack,
      net.minecraft.server.level.ServerLevel level,
      net.minecraft.world.entity.Entity owner,
      net.minecraft.world.entity.EquipmentSlot slot) {
    this.inventoryTick(
        stack,
        level,
        owner,
        ic2.compat.InventoryCompat.indexOf(owner, stack, slot),
        slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND);
  }

  public void inventoryTick(
      @NotNull ItemStack stack,
      @NotNull Level world,
      @NotNull Entity entity,
      int slot,
      boolean selected) {
    if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
      super.inventoryTick(
          stack,
          serverLevel,
          entity,
          selected ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : null);
    }
    if (entity instanceof Player player) {
      if (player.getItemBySlot(this.getEquipmentSlot()) == stack) {
        CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
        boolean active = nbtData.getBooleanOr("active", false);
        byte toggleTimer = nbtData.getByteOr("toggle_timer", (byte) 0);
        if (IC2.keyboard.isAltKeyDown(player)
            && IC2.keyboard.isModeSwitchKeyDown(player)
            && toggleTimer == 0) {
          toggleTimer = 10;
          active = !active;
          if (IC2.sideProxy.isSimulating()) {
            StackUtil.setTagValue(
                stack, nbtData, "active", net.minecraft.nbt.ByteTag.valueOf(active));
            if (active) {
              IC2.sideProxy.messagePlayer(player, "ic2.night_vision.mode.enabled");
            } else {
              IC2.sideProxy.messagePlayer(player, "ic2.night_vision.mode.disabled");
            }
          }
        }

        ItemArmorNanoSuit.getNightVisionOrNot(stack, player, nbtData, toggleTimer, active);
      }
    }
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
    Ic2Tooltip.add(
        tooltip,
        Component.translatable(
            "item.ic2.tooltip.night_vision.toggle",
            KeyboardClient.altKey.getKey().getDisplayName(),
            KeyboardClient.modeSwitchKey.getKey().getDisplayName()));
  }

  public boolean isValidRepairItem(
      @NotNull ItemStack par1ItemStack, @NotNull ItemStack par2ItemStack) {
    return false;
  }
}
