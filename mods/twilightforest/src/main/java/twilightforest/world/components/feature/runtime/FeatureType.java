package twilightforest.world.components.feature.runtime;

import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Couples one configured codec registration to its stateless TF algorithm. */
public final class FeatureType<C> implements Supplier<ConfiguredFeatureBase<C>> {
    private ConfiguredFeatureBase<C> algorithm;
    private final Supplier<? extends ConfiguredFeatureBase<C>> factory;

    private FeatureType(Supplier<? extends ConfiguredFeatureBase<C>> factory) {
        this.factory = Objects.requireNonNull(factory);
    }

    public static <C> FeatureType<C> register(DeferredRegister<MapCodec<? extends Feature>> registry,
                                             String name, Supplier<? extends ConfiguredFeatureBase<C>> factory) {
        var type = new FeatureType<C>(factory);
        registry.register(name, () -> type.get().configuredCodec());
        return type;
    }

    @Override
    public synchronized ConfiguredFeatureBase<C> get() {
        if (algorithm == null) algorithm = Objects.requireNonNull(factory.get());
        return algorithm;
    }
}
