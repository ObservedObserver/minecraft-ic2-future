package ic2.forge;

import ic2.api.item.INanoSaberState;
import ic2.core.util.StackUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

final class NanoSaberStateImpl implements INanoSaberState {
  private static final String NBT_ACTIVE = "active";

  private final ItemStack stack;
  private int energyTick;

  NanoSaberStateImpl(ItemStack stack) {
    this.stack = stack;
  }

  @Override
  public boolean isActive() {
    CompoundTag nbt = StackUtil.getTag(this.stack);
    return nbt != null && nbt.getBooleanOr(NBT_ACTIVE, false);
  }

  @Override
  public void setActive(boolean active) {
    StackUtil.setTagValue(this.stack, NBT_ACTIVE, net.minecraft.nbt.ByteTag.valueOf(active));
  }

  @Override
  public int getEnergyTick() {
    return this.energyTick;
  }

  @Override
  public void setEnergyTick(int energyTick) {
    this.energyTick = energyTick;
  }
}
