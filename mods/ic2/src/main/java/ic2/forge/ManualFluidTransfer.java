package ic2.forge;

import ic2.core.block.machine.tileentity.TileEntitySteamGenerator;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/** Fills a steam generator before a sneaking bucket/cell use can place fluid in the world. */
final class ManualFluidTransfer {
  private ManualFluidTransfer() {}

  static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
    var player = event.getEntity();
    var level = event.getLevel();
    var pos = event.getPos();
    var side = event.getHitVec().getDirection();
    ItemStack held = player.getItemInHand(event.getHand());
    if (event.isCanceled()
        || event.getUseBlock() == TriState.FALSE
        || event.getUseItem() == TriState.FALSE
        || !player.isShiftKeyDown()
        || player.isSpectator()
        || held.isEmpty()
        || !(held.getItem() instanceof BucketItem)
        || !(level.getBlockEntity(pos) instanceof TileEntitySteamGenerator)) {
      return;
    }

    var source = ItemAccess.forPlayerInteraction(player, event.getHand())
        .oneByOne().getCapability(Capabilities.Fluid.ITEM);
    if (source == null) return;
    FluidResource fluid = FluidResource.EMPTY;
    int index = -1;
    int amount = 0;
    for (int i = 0; i < source.size(); i++) {
      if (!source.getResource(i).isEmpty() && source.getAmountAsInt(i) > 0) {
        fluid = source.getResource(i);
        amount = source.getAmountAsInt(i);
        index = i;
        break;
      }
    }
    if (index < 0) return;

    // Claim the intent on both sides, including rejected fills. Client success still sends the
    // use-on packet, but stops a second use-item packet from pouring the bucket beside the tank.
    event.setCanceled(true);
    event.setCancellationResult(InteractionResult.SUCCESS);
    if (level.isClientSide()) return;
    event.setCancellationResult(InteractionResult.FAIL);
    if (!level.mayInteract(player, pos)
        || !level.getWorldBorder().isWithinBounds(pos)
        || !player.mayUseItemAt(pos, side, held)) {
      return;
    }
    var destination = level.getCapability(Capabilities.Fluid.BLOCK, pos, side);
    if (destination == null) return;

    Item usedItem = held.getItem();
    try (var transaction = Transaction.open(Transaction.getCurrentOpenedTransaction())) {
      // Classic cells cannot retain a partial remainder. Roll back the tank too if a complete
      // container does not fit, or its inventory exchange cannot complete.
      if (destination.insert(fluid, amount, transaction) != amount) return;
      if (!player.hasInfiniteMaterials()
          && source.extract(index, fluid, amount, transaction) != amount) return;
      transaction.commit();
    }

    player.getInventory().setChanged();
    player.containerMenu.broadcastChanges();
    player.awardStat(Stats.ITEM_USED.get(usedItem));
    level.playSound(null, pos,
        fluid.getFluid().is(net.minecraft.tags.FluidTags.LAVA)
            ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY,
        SoundSource.BLOCKS, 1.0F, 1.0F);
    level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
    event.setCancellationResult(InteractionResult.SUCCESS);
  }
}
