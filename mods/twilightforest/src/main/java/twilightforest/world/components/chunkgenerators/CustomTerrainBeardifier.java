package twilightforest.world.components.chunkgenerators;

import java.util.List;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.densityfunction.*;

/** The chunk's native beardifier plus independently compiled TF structure samplers. */
public final class CustomTerrainBeardifier extends Beardifier {
    private final Beardifier vanilla;
    private final List<DensitySampler> customDensities;
    public CustomTerrainBeardifier(Beardifier vanilla, List<DensitySampler> customDensities) {
        super(List.of(), List.of(), null);
        this.vanilla = vanilla;
        this.customDensities = List.copyOf(customDensities);
    }
    @Override public float sampleValue(SamplerContext context, int x, int y, int z) {
        double density = vanilla.sampleValue(context, x, y, z);
        for (DensitySampler sampler : customDensities) density += sampler.sampleValue(context, x, y, z);
        return (float) density;
    }
    @Override public void sampleVolume(SamplerContext context, DensityBuffer output, DensityVolume volume) {
        DensitySampler.sampleVolumeNaive(context, output, volume, this);
    }
}
