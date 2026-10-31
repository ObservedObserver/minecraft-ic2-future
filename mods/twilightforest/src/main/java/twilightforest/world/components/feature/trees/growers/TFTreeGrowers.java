package twilightforest.world.components.feature.trees.growers;

import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;
import twilightforest.init.TFConfiguredFeatures;

public class TFTreeGrowers {
    // The original 10% secondary-tree chances become 9:1 native weighted lists.
    public static final TreeGrower TWILIGHT_OAK = new TreeGrower("twilight_oak",
        WeightedList.of(new Weighted<>(TFConfiguredFeatures.TWILIGHT_OAK_TREE, 9), new Weighted<>(TFConfiguredFeatures.LARGE_TWILIGHT_OAK_TREE, 1)),
        WeightedList.of(new Weighted<>(TFConfiguredFeatures.FOREST_MEGA_OAK_TREE, 9), new Weighted<>(TFConfiguredFeatures.SAVANNAH_MEGA_OAK_TREE, 1)),
        WeightedList.of(), TFConfiguredFeatures.TWILIGHT_OAK_TREE);
    public static final TreeGrower CANOPY = new TreeGrower("canopy", WeightedList.of(TFConfiguredFeatures.CANOPY_TREE),
        WeightedList.of(TFConfiguredFeatures.MEGA_CANOPY_TREE), WeightedList.of(), TFConfiguredFeatures.CANOPY_TREE);
    public static final TreeGrower MANGROVE = single("mangrove", TFConfiguredFeatures.MANGROVE_TREE);
    public static final TreeGrower DARK = single("dark", TFConfiguredFeatures.HOMEGROWN_DARKWOOD_TREE);
    public static final TreeGrower TIME = single("time", TFConfiguredFeatures.TIME_TREE);
    public static final TreeGrower TRANSFORMATION = single("transformation", TFConfiguredFeatures.TRANSFORMATION_TREE);
    public static final TreeGrower MINING = single("mining", TFConfiguredFeatures.MINING_TREE);
    public static final TreeGrower SORTING = single("sorting", TFConfiguredFeatures.SORTING_TREE);
    public static final TreeGrower HOLLOW_OAK = new StructureTreeGrower("hollow_oak");
    public static final TreeGrower RAINBOW_OAK = new TreeGrower("rainbow_oak",
        WeightedList.of(new Weighted<>(TFConfiguredFeatures.RAINBOW_OAK_TREE, 9), new Weighted<>(TFConfiguredFeatures.LARGE_RAINBOW_OAK_TREE, 1)),
        WeightedList.of(), WeightedList.of(), TFConfiguredFeatures.RAINBOW_OAK_TREE);

    private static TreeGrower single(String name, ResourceKey<Feature> feature) {
        return new TreeGrower(name, WeightedList.of(feature), WeightedList.of(), WeightedList.of(), feature);
    }
}
