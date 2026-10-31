package twilightforest.world.components.carver;

import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.world.level.ChunkPos;
import twilightforest.world.components.CarverWallProvider;
import twilightforest.world.components.TFCavesCarver;

/** A stable codec per TF cave variant; configurations are native WorldCarvers. */
public final class TFCaveCarverType {
    public final boolean highlands;
    public final CarverWallProvider walls;
    public final Predicate<ChunkPos> highlandPlacement;
    public final MapCodec<TFCavesCarver> codec;

    public TFCaveCarverType(boolean highlands, CarverWallProvider walls, Predicate<ChunkPos> highlandPlacement) {
        this.highlands = highlands;
        this.walls = walls;
        this.highlandPlacement = highlandPlacement;
        this.codec = TFCaveConfiguration.CODEC.fieldOf("config").xmap(this::configured, TFCavesCarver::configuration);
    }

    public TFCavesCarver configured(TFCaveConfiguration configuration) { return new TFCavesCarver(this, configuration); }
}
