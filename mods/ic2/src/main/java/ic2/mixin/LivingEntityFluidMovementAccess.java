package ic2.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Allows IC2 fluid types to reuse the native physics without replacing entity travel. */
@Mixin(LivingEntity.class)
public interface LivingEntityFluidMovementAccess {
  @Invoker("travelInAir")
  void ic2$travelInAir(Vec3 input);

  @Invoker("travelInWater")
  void ic2$travelInWater(Vec3 input, double gravity, boolean falling, double previousY);

  @Invoker("travelInLava")
  void ic2$travelInLava(Vec3 input, double gravity, boolean falling, double previousY);
}
