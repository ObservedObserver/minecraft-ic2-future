package ic2.core.world;

import ic2.core.IC2;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Features and biome placement are defined by the mod's 26.3 datapack resources. */
public final class Ic2WorldGen {
  public static final ResourceKey<Feature> RUBBER_TREE =
      ResourceKey.create(Registries.FEATURE, IC2.getIdentifier("rubber_tree"));

  private Ic2WorldGen() {}

  public static void init() {
    RubberTreeFoliagePlacer.init();
  }
}
