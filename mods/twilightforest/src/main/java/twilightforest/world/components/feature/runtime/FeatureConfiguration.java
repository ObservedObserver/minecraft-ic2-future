package twilightforest.world.components.feature.runtime;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Optional child-feature discovery for TF's configuration objects. */
public interface FeatureConfiguration {
    NoneFeatureConfiguration NONE = NoneFeatureConfiguration.NONE;
    default Stream<Holder<Feature>> getSubFeatures() { return Stream.empty(); }
}
