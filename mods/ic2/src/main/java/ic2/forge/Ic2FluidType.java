package ic2.forge;

import ic2.mixin.LivingEntityFluidMovementAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/** Explicit travel handlers for the custom-fluid branch in Minecraft 26.3. */
public final class Ic2FluidType extends FluidType {
  private enum Movement { WATER, LIQUID, LAVA, GAS }

  private final Movement movement;

  private Ic2FluidType(Properties properties, Movement movement) {
    super(properties);
    this.movement = movement;
  }

  static FluidType create(Identifier id, Properties properties, boolean gaseous) {
    Movement movement = gaseous ? Movement.GAS : switch (id.getPath()) {
      case "distilled_water", "hot_water", "heavy_water" -> Movement.WATER;
      case "pahoehoe_lava" -> Movement.LAVA;
      default -> Movement.LIQUID;
    };
    if (movement == Movement.WATER) {
      properties.isWaterLike(true).canExtinguish(true).fallDistanceModifier(0.0F);
    } else if (movement == Movement.GAS) {
      // The explicit gas flag matters: biogas has a positive density, oxygen has density zero.
      properties.canSwim(false).canDrown(false).canPushEntity(false)
          .fallDistanceModifier(1.0F).pathType(null).adjacentPathType(null);
    } else if (movement == Movement.LAVA) {
      properties.canSwim(false).canDrown(false);
    }
    return new Ic2FluidType(properties, movement);
  }

  @Override
  public boolean move(LivingEntity entity, Vec3 input, double gravity) {
    if (this.movement == Movement.WATER || entity.isInWater() || entity.isInLava()) {
      return false; // Preserve native water/lava priority when several fluids touch the entity.
    }
    if (this.movement == Movement.GAS && entity.getFluidInteraction().isInFluidMatching(
        entity, (touching, type, height) -> type instanceof Ic2FluidType other
            && other.movement != Movement.GAS)) {
      return false; // A gas around the head must not override liquid movement at the feet.
    }
    var nativeMovement = (LivingEntityFluidMovementAccess) entity;
    switch (this.movement) {
      case GAS -> nativeMovement.ic2$travelInAir(input);
      case LAVA -> nativeMovement.ic2$travelInLava(
          input, gravity, entity.getDeltaMovement().y <= 0.0, entity.getY());
      case LIQUID -> nativeMovement.ic2$travelInWater(
          input, gravity, entity.getDeltaMovement().y <= 0.0, entity.getY());
      default -> throw new IllegalStateException("Unhandled fluid movement " + this.movement);
    }
    return true;
  }

  /** Ignore gaseous volumes when the native jump code asks for a liquid to swim upwards in. */
  public static FluidType jumpFluid(LivingEntity entity, FluidType maximum) {
    if (!(maximum instanceof Ic2FluidType fluid) || fluid.movement != Movement.GAS) {
      return maximum;
    }
    FluidType[] selected = {net.neoforged.neoforge.common.NeoForgeMod.EMPTY_TYPE.value()};
    double[] height = {0.0};
    entity.getFluidInteraction().isInFluidMatching(entity, (touching, type, depth) -> {
      if (!(type instanceof Ic2FluidType other && other.movement == Movement.GAS)
          && depth > height[0]) {
        selected[0] = type;
        height[0] = depth;
      }
      return false;
    });
    return selected[0];
  }
}
