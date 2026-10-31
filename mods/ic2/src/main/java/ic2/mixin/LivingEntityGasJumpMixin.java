package ic2.mixin;

import ic2.forge.Ic2FluidType;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Gas at the feet is air for jumping; liquid below a gas layer still permits swimming. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityGasJumpMixin {
  @Redirect(method = "aiStep", at = @At(value = "INVOKE", target =
      "Lnet/minecraft/world/entity/EntityFluidInteraction;getMaxHeightFluidType()Lnet/neoforged/neoforge/fluids/FluidType;"))
  private FluidType ic2$jumpFluid(EntityFluidInteraction interaction) {
    return Ic2FluidType.jumpFluid((LivingEntity) (Object) this, interaction.getMaxHeightFluidType());
  }
}
