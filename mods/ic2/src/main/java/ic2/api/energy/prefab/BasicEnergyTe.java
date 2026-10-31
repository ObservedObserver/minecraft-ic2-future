package ic2.api.energy.prefab;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class BasicEnergyTe<T extends BasicEnergyTile> extends BlockEntity {
  protected T energyBuffer;

  protected BasicEnergyTe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
  }

  public T getEnergyBuffer() {
    return this.energyBuffer;
  }

  public void setRemoved() {
    super.setRemoved();
    this.energyBuffer.invalidate();
  }

  @Override
  protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
    super.loadAdditional(input);
    CompoundTag tag =
        input
            .read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC))
            .orElseGet(CompoundTag::new);
    this.loadAdditional(tag, input.lookup());
  }

  @Override
  protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
    super.saveAdditional(output);
    CompoundTag tag = new CompoundTag();
    this.saveAdditional(
        tag,
        this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY);
    output.store(tag);
  }

  protected void loadAdditional(
      CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    this.energyBuffer.readFromNBT(nbt);
  }

  protected void saveAdditional(
      CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    this.energyBuffer.writeToNBT(nbt);
  }

  public static class Sink extends BasicEnergyTe<BasicSink> {
    public Sink(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int tier) {
      super(type, pos, state);
      this.energyBuffer = new BasicSink(this, capacity, tier);
    }
  }

  public static class Source extends BasicEnergyTe<BasicSource> {
    public Source(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int tier) {
      super(type, pos, state);
      this.energyBuffer = new BasicSource(this, capacity, tier);
    }
  }
}
