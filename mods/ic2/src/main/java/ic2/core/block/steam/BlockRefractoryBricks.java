package ic2.core.block.steam;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

public class BlockRefractoryBricks extends Block {

  public BlockRefractoryBricks(
      net.minecraft.world.level.block.state.BlockBehaviour.Properties properties) {
    super(properties);
  }

  public BlockRefractoryBricks() {
    super(
        Properties.of()
            .setId(
                net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.BLOCK,
                    ic2.core.IC2.getIdentifier("refractory_bricks")))
            .strength(2.0F, 10.0F)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE));
  }
}
