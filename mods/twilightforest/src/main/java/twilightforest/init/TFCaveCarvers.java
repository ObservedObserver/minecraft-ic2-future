package twilightforest.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import com.mojang.serialization.MapCodec;
import twilightforest.world.components.carver.TFCaveCarverType;
import twilightforest.util.landmarks.LegacyLandmarkPlacements;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.neoforged.neoforge.registries.DeferredRegister;
import twilightforest.TwilightForestMod;
import twilightforest.world.components.NoiseCarverWallProvider;

import java.util.List;

//this was all put into 1 class because it seems like a waste to have it in 2
public class TFCaveCarvers {
	public static final DeferredRegister<MapCodec<? extends WorldCarver>> CARVER_TYPES = DeferredRegister.create(Registries.CARVER_TYPE, TwilightForestMod.ID);

	public static final TFCaveCarverType TF_CAVES = new TFCaveCarverType(
		false,
		new NoiseCarverWallProvider(
			6972119253061020355L,
			NormalNoise.createParity(0, 1.0),
			0.5f,
			List.of(
				Blocks.DIRT.defaultBlockState(),
				Blocks.DIRT.defaultBlockState(),
				Blocks.ROOTED_DIRT.defaultBlockState(),
				Blocks.DIRT.defaultBlockState(),
				Blocks.DIRT.defaultBlockState(),
				Blocks.COARSE_DIRT.defaultBlockState(),
				Blocks.DIRT.defaultBlockState(),
				Blocks.DIRT.defaultBlockState()
			)
		)
	, _ -> true);
	public static final TFCaveCarverType HIGHLAND_CAVES = highlands();
	private static TFCaveCarverType highlands() {


		return new TFCaveCarverType(
			true,
			(random, _) -> random.nextInt(4) == 0 ? TFBlocks.TROLLSTEINN.value().defaultBlockState() : Blocks.STONE.defaultBlockState(),
            pos -> (Mth.clamp(LegacyLandmarkPlacements.manhattanDistanceFromLandmarkCenter(pos.x(), pos.z()), 0, 3) & 1) == 0
		);
	}
    static {
        CARVER_TYPES.register("tf_caves", () -> TF_CAVES.codec);
        CARVER_TYPES.register("highland_caves", () -> HIGHLAND_CAVES.codec);
    }

	public static final ResourceKey<WorldCarver> TFCAVES_CONFIGURED = registerKey("tf_caves");
	public static final ResourceKey<WorldCarver> HIGHLANDCAVES_CONFIGURED = registerKey("highland_caves");

	private static ResourceKey<WorldCarver> registerKey(String name) {
		return ResourceKey.create(Registries.CARVER, TwilightForestMod.prefix(name));
	}

}
