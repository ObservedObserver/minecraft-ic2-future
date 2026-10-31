package ic2.core.item.tool;

import ic2.compat.Tiers;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public class ItemElectricToolHoe extends ItemElectricTool {
  private static final float MINING_SPEED = 16.0F;

  public ItemElectricToolHoe(Properties settings) {
    super(
        settings.delayedComponent(
            net.minecraft.core.component.DataComponents.BLOCK_TRANSFORMER,
            context ->
                context.getOrThrow(net.minecraft.world.item.component.BlockTransformers.HOE)),
        50,
        Tiers.IRON,
        List.of(BlockTags.MINEABLE_WITH_HOE));
    this.maxCharge = 10000;
    this.transferLimit = 100;
    this.tier = 1;
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    ItemStack stack = context.getItemInHand();
    if (!this.canUse(stack) || context.getClickedFace() == Direction.DOWN) {
      return InteractionResult.PASS;
    }

    var holder = stack.get(net.minecraft.core.component.DataComponents.BLOCK_TRANSFORMER);
    if (holder == null) return InteractionResult.PASS;
    // Reuse vanilla and data-map hoe rules, with electric energy in place of durability.
    var transforms =
        java.util.stream.StreamSupport.stream(
                net.neoforged.neoforge.common.DataMapHooks.appendDatamapTransformers(
                        holder, holder.value().transforms())
                    .spliterator(),
                false)
            .map(
                data ->
                    new net.minecraft.core.component.BlockTransformer.BlockTransformData(
                        data.blockStateProvider(),
                        data.sound(),
                        data.particle(),
                        data.disallowedFaces(),
                        data.loot(),
                        data.dropStrategy(),
                        data.updateFromNeighbors(),
                        data.transformType(),
                        false,
                        0))
            .toList();
    InteractionResult result =
        new net.minecraft.core.component.BlockTransformer(transforms).transformBlock(context, null);
    if (!context.getLevel().isClientSide() && result.consumesAction()) {
      this.consumeEnergy(stack, this.operationEnergyCost, context.getPlayer());
    }
    return result;
  }

  @Override
  public float getDestroySpeed(ItemStack stack, BlockState state) {
    float speed = super.getDestroySpeed(stack, state);
    return speed == 1.0F ? speed : speed * MINING_SPEED / Tiers.IRON.getSpeed();
  }
}
