package ic2.core.entity.boat;

import ic2.api.entity.boat.AbstractBoatEntity;
import ic2.api.entity.boat.BoatType;
import ic2.core.ref.Ic2BoatTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CarbonBoatEntity extends AbstractBoatEntity {
  public CarbonBoatEntity(EntityType<? extends AbstractBoatEntity> entityType, Level world) {
    super(entityType, world);
  }

  @SuppressWarnings("unused")
  public CarbonBoatEntity(
      EntityType<? extends AbstractBoatEntity> entityType,
      Level world,
      double x,
      double y,
      double z) {
    /* This method are used by reflection. */
    super(entityType, world, x, y, z);
  }

  @Override
  public boolean brokenByFalling() {
    return false;
  }

  @Override
  public BoatType getOverrideBoatType() {
    return Ic2BoatTypes.CARBON;
  }

  @Override
  protected float getBlockSpeedFactor() {
    return super.getBlockSpeedFactor() * 1.15f;
  }

  public void tick() {
    super.tick();
    if (!this.level().isClientSide()
        && this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
      this.hurt(this.damageSources().lava(), Float.MAX_VALUE);
    }
  }
}
