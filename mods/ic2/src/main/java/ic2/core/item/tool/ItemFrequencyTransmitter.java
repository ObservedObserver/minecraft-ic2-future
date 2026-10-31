package ic2.core.item.tool;

import ic2.compat.InteractionResults;
import ic2.core.IC2;
import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.util.Ic2Tooltip;
import ic2.core.util.StackUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class ItemFrequencyTransmitter extends Item {
  public ItemFrequencyTransmitter(Properties settings) {
    super(settings);
  }

  public @NotNull InteractionResult use(
      @NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
    ItemStack stack = StackUtil.get(player, hand);
    if (IC2.sideProxy.isSimulating()) {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
      boolean hadJustSet = nbtData.getBooleanOr("targetJustSet", false);
      if (nbtData.getBooleanOr("targetSet", false) && !hadJustSet) {
        StackUtil.setTagValue(
            stack, nbtData, "targetSet", net.minecraft.nbt.ByteTag.valueOf(false));
        IC2.sideProxy.messagePlayer(player, "ic2.frequency_transmitter.unlink_target");
      }

      if (hadJustSet) {
        StackUtil.setTagValue(
            stack, nbtData, "targetJustSet", net.minecraft.nbt.ByteTag.valueOf(false));
      }
    }

    return InteractionResults.withStack(InteractionResult.SUCCESS, stack);
  }

  public @NotNull InteractionResult useOn(UseOnContext context) {
    Level world = context.getLevel();
    Player player = context.getPlayer();
    BlockPos pos = context.getClickedPos();
    InteractionHand hand = context.getHand();
    if (player == null) {
      return InteractionResult.PASS;
    } else if (world.isClientSide()) {
      return InteractionResult.PASS;
    } else if (!(world.getBlockEntity(pos) instanceof TileEntityTeleporter tp)) {
      return InteractionResult.PASS;
    } else {
      CompoundTag nbtData = StackUtil.getOrCreateNbtData(StackUtil.get(player, hand));
      boolean targetSet = nbtData.getBooleanOr("targetSet", false);
      boolean justSetTarget = true;
      BlockPos target =
          new BlockPos(
              nbtData.getIntOr("targetX", 0),
              nbtData.getIntOr("targetY", 0),
              nbtData.getIntOr("targetZ", 0));
      if (!targetSet) {
        targetSet = true;
        target = tp.getBlockPos();
        IC2.sideProxy.messagePlayer(
            player,
            "ic2.frequency_transmitter.link_target",
            target.getX(),
            target.getY(),
            target.getZ());
      } else if (tp.getBlockPos().equals(target)) {
        IC2.sideProxy.messagePlayer(player, "ic2.frequency_transmitter.cannot_link_to_self");
      } else if (tp.hasTarget() && tp.getTarget().equals(target)) {
        IC2.sideProxy.messagePlayer(player, "ic2.frequency_transmitter.link_already_established");
      } else {
        BlockEntity targetTe = world.getBlockEntity(target);
        if (targetTe instanceof TileEntityTeleporter) {
          tp.setTarget(target);
          ((TileEntityTeleporter) targetTe).setTarget(pos);
          IC2.sideProxy.messagePlayer(
              player,
              "ic2.frequency_transmitter.link_target",
              target.getX(),
              target.getY(),
              target.getZ());
        } else {
          justSetTarget = false;
          targetSet = false;
        }
      }

      StackUtil.setTagValue(
          StackUtil.get(player, hand),
          nbtData,
          "targetSet",
          net.minecraft.nbt.ByteTag.valueOf(targetSet));
      StackUtil.setTagValue(
          StackUtil.get(player, hand),
          nbtData,
          "targetJustSet",
          net.minecraft.nbt.ByteTag.valueOf(justSetTarget));
      StackUtil.setTagValue(
          StackUtil.get(player, hand),
          nbtData,
          "targetX",
          net.minecraft.nbt.IntTag.valueOf(target.getX()));
      StackUtil.setTagValue(
          StackUtil.get(player, hand),
          nbtData,
          "targetY",
          net.minecraft.nbt.IntTag.valueOf(target.getY()));
      StackUtil.setTagValue(
          StackUtil.get(player, hand),
          nbtData,
          "targetZ",
          net.minecraft.nbt.IntTag.valueOf(target.getZ()));
      return InteractionResult.SUCCESS;
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
      @NotNull TooltipFlag advanced) {
    CompoundTag nbtData = StackUtil.getOrCreateNbtData(stack);
    if (nbtData.getBooleanOr("targetSet", false)) {
      Ic2Tooltip.add(
          tooltip,
          Component.translatable(
              "ic2.frequency_transmitter.tooltip.target",
              nbtData.getIntOr("targetX", 0),
              nbtData.getIntOr("targetY", 0),
              nbtData.getIntOr("targetZ", 0)));
    } else {
      Ic2Tooltip.add(tooltip, Component.translatable("ic2.frequency_transmitter.tooltip.blank"));
    }
  }
}
