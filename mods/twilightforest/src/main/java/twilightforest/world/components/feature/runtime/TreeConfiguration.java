package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.rootplacers.RootPlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;

/** TF tree configuration with an explicit conversion to the native 26.3 tree. */
public final class TreeConfiguration implements FeatureConfiguration {
    public final BlockStateProvider trunkProvider, foliageProvider, belowTrunkProvider;
    public final TrunkPlacer trunkPlacer;
    public final FoliagePlacer foliagePlacer;
    public final Optional<RootPlacer> rootPlacer;
    public final FeatureSize minimumSize;
    public final List<TreeDecorator> decorators;
    public final boolean ignoreVines;

    public static final BlockPredicate CAN_PLACE_BELOW_OVERWORLD_TRUNKS = BlockPredicate.not(
            BlockPredicate.matchesTag(BlockTags.CANNOT_REPLACE_BELOW_TREE_TRUNK));
    public static final BlockStateProvider PLACE_BELOW_OVERWORLD_TRUNKS = RuleBasedStateProvider.ifTrueThenProvide(
            CAN_PLACE_BELOW_OVERWORLD_TRUNKS, Blocks.DIRT);
    public static final Codec<TreeConfiguration> CODEC = TreeFeature.CODEC.codec()
            .xmap(TreeConfiguration::fromNative, TreeConfiguration::nativeFeature);

    public TreeConfiguration(BlockStateProvider trunkProvider, TrunkPlacer trunkPlacer,
                             BlockStateProvider foliageProvider, FoliagePlacer foliagePlacer,
                             Optional<RootPlacer> rootPlacer, FeatureSize minimumSize,
                             List<TreeDecorator> decorators, boolean ignoreVines,
                             BlockStateProvider belowTrunkProvider) {
        this.trunkProvider = trunkProvider;
        this.trunkPlacer = trunkPlacer;
        this.foliageProvider = foliageProvider;
        this.foliagePlacer = foliagePlacer;
        this.rootPlacer = rootPlacer;
        this.belowTrunkProvider = belowTrunkProvider;
        this.minimumSize = minimumSize;
        this.decorators = List.copyOf(decorators);
        this.ignoreVines = ignoreVines;
    }

    public TreeFeature nativeFeature() {
        return new TreeFeature(Holder.direct(trunkProvider), trunkPlacer, Holder.direct(foliageProvider),
                foliagePlacer, rootPlacer, minimumSize, decorators, ignoreVines, Holder.direct(belowTrunkProvider));
    }

    public static TreeConfiguration fromNative(TreeFeature tree) {
        return new TreeConfiguration(tree.trunkProvider().value(), tree.trunkPlacer(), tree.foliageProvider().value(),
                tree.foliagePlacer(), tree.rootPlacer(), tree.minimumSize(), tree.decorators(), tree.ignoreVines(),
                tree.belowTrunkProvider().value());
    }

    public static final class TreeConfigurationBuilder {
        private final BlockStateProvider trunk, foliage;
        private final TrunkPlacer trunkPlacer;
        private final FoliagePlacer foliagePlacer;
        private final FeatureSize size;
        private BlockStateProvider below = PLACE_BELOW_OVERWORLD_TRUNKS;
        private Optional<RootPlacer> root = Optional.empty();
        private List<TreeDecorator> decorators = List.of();
        private boolean ignoreVines;

        public TreeConfigurationBuilder(BlockStateProvider trunk, TrunkPlacer trunkPlacer,
                                        BlockStateProvider foliage, FoliagePlacer foliagePlacer, FeatureSize size) {
            this.trunk = trunk; this.trunkPlacer = trunkPlacer;
            this.foliage = foliage; this.foliagePlacer = foliagePlacer; this.size = size;
        }

        public TreeConfigurationBuilder(BlockStateProvider trunk, TrunkPlacer trunkPlacer,
                                        BlockStateProvider foliage, FoliagePlacer foliagePlacer,
                                        Optional<RootPlacer> root, FeatureSize size, BlockStateProvider below) {
            this(trunk, trunkPlacer, foliage, foliagePlacer, size);
            this.root = root; this.below = below;
        }

        public TreeConfigurationBuilder belowTrunkProvider(BlockStateProvider value) { below = value; return this; }
        public TreeConfigurationBuilder decorators(List<TreeDecorator> value) { decorators = List.copyOf(value); return this; }
        public TreeConfigurationBuilder ignoreVines() { ignoreVines = true; return this; }
        public TreeConfigurationBuilder rootPlacer(RootPlacer value) { root = Optional.of(value); return this; }
        public TreeConfiguration build() {
            return new TreeConfiguration(trunk, trunkPlacer, foliage, foliagePlacer, root, size, decorators, ignoreVines, below);
        }
    }
}
