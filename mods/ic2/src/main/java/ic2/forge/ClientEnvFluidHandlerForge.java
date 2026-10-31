package ic2.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

public class ClientEnvFluidHandlerForge extends EnvFluidHandlerForge {
  private static FluidModel model(Fluid fluid) {
    return Minecraft.getInstance()
        .getModelManager()
        .getFluidStateModelSet()
        .get(fluid.defaultFluidState());
  }

  @Override
  public Identifier getStillSpriteId(Fluid fluid) {
    return model(fluid).stillMaterial().sprite().contents().name();
  }

  @Override
  public Identifier getFlowingSpriteId(Fluid fluid) {
    return model(fluid).flowingMaterial().sprite().contents().name();
  }

  @Override
  public int getColor(Fluid fluid) {
    var tint = model(fluid).fluidTintSource();
    return tint == null ? -1 : tint.color(fluid.defaultFluidState());
  }
}
