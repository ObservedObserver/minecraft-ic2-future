package ic2.core.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.level.block.state.BlockState;

public final class ModelUtil {
  private ModelUtil() {}

  public static String getVariant(BlockState state) {
    return ic2.core.block.state.BlockStateUtil.getVariantString(state);
  }

  public static BlockStateModel getMissingModel() {
    return manager().getBlockStateModelSet().missingModel();
  }

  public static BlockStateModel getBlockModel(BlockState state) {
    return manager().getBlockStateModelSet().get(state);
  }

  private static ModelManager manager() {
    return Minecraft.getInstance().getModelManager();
  }
}
