package twilightforest.world.components.structures.placements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import twilightforest.util.landmarks.LandmarkGridMath;

import java.util.Optional;

public class AvoidLandmarkGridPlacement extends RandomSpreadStructurePlacement {
	// Widen the codec result to match the native superclass signature while retaining
	// this registered codec identity and the subclass instance on decode.
	public static final MapCodec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.<AvoidLandmarkGridPlacement>mapCodec(instance -> instance.group(
		Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(AvoidLandmarkGridPlacement::locateOffset),
		AbstractSpreadingStructurePlacement.FrequencyReductionMethod.CODEC
			.optionalFieldOf("frequency_reduction_method", AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT)
			.forGetter(AvoidLandmarkGridPlacement::frequencyReductionMethod),
		Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(AvoidLandmarkGridPlacement::frequency),
		ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(AvoidLandmarkGridPlacement::salt),
		AbstractSpreadingStructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(AvoidLandmarkGridPlacement::exclusionZone),
		Codec.intRange(0, 4096).fieldOf("spacing").forGetter(AvoidLandmarkGridPlacement::spacing),
		Codec.intRange(0, 4096).fieldOf("separation").forGetter(AvoidLandmarkGridPlacement::separation),
		RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(AvoidLandmarkGridPlacement::spreadType),
		AvoidAdditionalStructures.CODEC.optionalFieldOf("avoid_additional_structures").forGetter(AvoidLandmarkGridPlacement::otherStructuresToAvoid)
	).apply(instance, AvoidLandmarkGridPlacement::new)).validate(AvoidLandmarkGridPlacement::validate)
		.xmap(placement -> placement, AvoidLandmarkGridPlacement.class::cast);

	private final Optional<AvoidAdditionalStructures> avoidOtherStructures;

	private static DataResult<AvoidLandmarkGridPlacement> validate(AvoidLandmarkGridPlacement placement) {
		return placement.spacing() <= placement.separation() ? DataResult.error(() -> "Spacing has to be larger than separation") : DataResult.success(placement);
	}

	public AvoidLandmarkGridPlacement(int spacing, int separation, RandomSpreadType spreadType, int salt, Optional<AvoidAdditionalStructures> avoidOtherStructures) {
		this(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty(), spacing, separation, spreadType, avoidOtherStructures);
	}

	public AvoidLandmarkGridPlacement(Vec3i locateOffset, FrequencyReductionMethod reduction, float frequencyModifier, int salt, Optional<ExclusionZone> exclusionZone, int spacing, int separation, RandomSpreadType spreadType, Optional<AvoidAdditionalStructures> avoidOtherStructures) {
		super(locateOffset, reduction, frequencyModifier, salt, exclusionZone, spacing, separation, spreadType);
		this.avoidOtherStructures = avoidOtherStructures;
	}

	@Override
	protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
		//copy of super
		ChunkPos chunkpos = this.getPotentialStructureChunk(state.getLevelSeed(), chunkX, chunkZ);
		if (chunkpos.x() != chunkX || chunkpos.z() != chunkZ) {
			return false;
		}

        return isOutsideLandmarkBuffer(chunkpos);
    }

    public static boolean isOutsideLandmarkBuffer(ChunkPos chunk) {
        BlockPos center = LandmarkGridMath.getNearestCenterXZ(chunk.x(), chunk.z());
        // Preserve the original comparison against the chunk minimum, including
        // its asymmetric +8/+9 center offset around negative coordinates.
        return Math.abs(center.getX() - chunk.getMinBlockX()) >= 80
            || Math.abs(center.getZ() - chunk.getMinBlockZ()) >= 80;
    }

	@Override
	public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState structureState, int x, int z) {
		return super.applyInteractionsWithOtherStructures(structureState, x, z)
			&& (this.avoidOtherStructures.isEmpty() || !this.avoidOtherStructures.get().isPlacementForbidden(structureState, x, z));

	}

	private Optional<AvoidAdditionalStructures> otherStructuresToAvoid() {
		return this.avoidOtherStructures;
	}

	@Override
	public MapCodec<RandomSpreadStructurePlacement> codec() { return CODEC; }

	public record AvoidAdditionalStructures(Object2IntArrayMap<Holder<StructureSet>> avoidStructures) {
		public static final Codec<AvoidAdditionalStructures> CODEC = Codec.unboundedMap(StructureSet.CODEC, Codec.intRange(0, 16)).xmap(map -> new AvoidAdditionalStructures(new Object2IntArrayMap<>(map)), AvoidAdditionalStructures::avoidStructures);

		private boolean isPlacementForbidden(ChunkGeneratorStructureState structureState, int x, int z) {
			for (Object2IntArrayMap.Entry<Holder<StructureSet>> structureSetEntry : this.avoidStructures.object2IntEntrySet()) {
				if (structureState.hasStructureChunkInRange(structureSetEntry.getKey(), x, z, structureSetEntry.getIntValue())) {
					return true;
				}
			}

			return false;
		}
	}
}
