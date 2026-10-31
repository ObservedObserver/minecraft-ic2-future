package ic2.mixin;

import ic2.core.ref.Ic2Items;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The final knight buffers group loot even when Twilight Forest boss chests are disabled. */
@Pseudo
@Mixin(targets = "twilightforest.entity.boss.KnightPhantom", remap = false)
public abstract class TwilightKnightRewardMixin extends Monster {
  protected TwilightKnightRewardMixin(EntityType<? extends Monster> type, Level level) {
    super(type, level);
  }
  @Shadow public abstract List<?> getNearbyKnights();

  @Inject(method = "postmortem", at = @At("RETURN"), remap = false)
  private void ic2$dropBufferedShardsWithoutBossChest(
      ServerLevel level, DamageSource cause, CallbackInfo callback) {
    // This virtual method is true only for ordinary ground loot. With boss chests on,
    // Twilight Forest will deposit the buffered reward into its normal final chest.
    if (!shouldDropLoot(level) || !getNearbyKnights().isEmpty()) return;
    var items = ((TwilightBossBufferAccess) this).ic2$getDyingInventory();
    for (int i = 0; i < items.size(); i++) {
      ItemStack stack = items.get(i);
      if (stack.is(Ic2Items.IRIDIUM_SHARD)) {
        if (((Entity) (Object) this).spawnAtLocation(level, stack.copy()) != null) {
          items.set(i, ItemStack.EMPTY);
        }
      }
    }
  }
}
