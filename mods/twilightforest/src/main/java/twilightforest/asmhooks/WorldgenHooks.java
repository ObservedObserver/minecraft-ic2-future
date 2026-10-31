package twilightforest.asmhooks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunctions;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import twilightforest.init.custom.ChunkBlanketProcessors;
import twilightforest.world.components.chunkgenerators.CustomTerrainBeardifier;
import twilightforest.world.components.structures.CustomDensitySource;
import twilightforest.world.components.structures.util.CustomStructureData;

import java.util.List;

@SuppressWarnings({"JavadocReference", "unused"})
public class WorldgenHooks {
	private static final java.util.concurrent.atomic.AtomicBoolean MISSING_REGION_REPORTED = new java.util.concurrent.atomic.AtomicBoolean();

	/**
	 * {@link twilightforest.asm.transformers.beardifier.InjectCustomTerrainBeardifierDuringCreateNoiseChunkTransformer}<p/>
	 *
	 * Injection point:<br/>
	 * {@link net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator#createNoiseChunk(ChunkAccess, StructureManager, Blender, RandomState)}
	 */
	public static Beardifier gatherCustomTerrain(Beardifier vanilla, StructureManager structureManager, ChunkAccess chunkAccess, RandomState randomState) {
		ChunkPos chunkPos = chunkAccess.getPos();
		List<StructureStart> structureStarts = structureManager.startsForStructure(chunkPos.x(), chunkPos.z(), s -> s instanceof CustomDensitySource);

		if (structureStarts.isEmpty())
			return vanilla;

		ObjectArrayList<net.minecraft.world.level.levelgen.densityfunction.DensitySampler> customStructureTerraforms = new ObjectArrayList<>(structureStarts.size());

		// These functions are chunk-specific. A local compiler avoids retaining every
		// generated structure in the RandomState-wide sampler cache.
		var compiler = new net.minecraft.world.level.levelgen.densityfunction.DensityFunctionCompiler(randomState.densityFunctionCompiler.context);

		for (StructureStart structureStart : structureStarts)
			if (structureStart.getStructure() instanceof CustomDensitySource customDensitySource)
				customStructureTerraforms.add(compiler.getSampler(customDensitySource.getStructureTerraformer(chunkPos, structureStart)));

		return customStructureTerraforms.isEmpty() ? vanilla : new CustomTerrainBeardifier(vanilla, customStructureTerraforms);
	}

	/**
	 * {@link twilightforest.asm.transformers.chunk.ChunkStatusTaskTransformer}<p/>
	 *
	 * Injection Point:<br/>
	 * The start of NoiseBasedChunkGenerator.generateCarvers, after buildTerrain has built the surface.
	 */
	public static void chunkBlanketing(ChunkAccess access, WorldGenRegion region) {
		// buildTerrain also supports callers without a live world-generation region.
		if (region != null) {
			ChunkBlanketProcessors.chunkBlanketing(access, region);
		} else if (MISSING_REGION_REPORTED.compareAndSet(false, true)) {
			twilightforest.TwilightForestMod.LOGGER.debug("Skipping TF coverings for terrain generation without WorldGenRegion at {}", access.getPos());
		}
	}

	/**
	 * {@link twilightforest.asm.transformers.conquered.StructureStartLoadStaticTransformer}<p/>
	 *
	 * Injection Point:<br/>
	 * {@link net.minecraft.world.level.levelgen.structure.StructureStart#loadStaticStart(StructurePieceSerializationContext, CompoundTag, long)}<br/>
	 * Targets: {@link net.minecraft.world.level.levelgen.structure.StructureStart#StructureStart(Structure, ChunkPos, int, PiecesContainer)}
	 */
	public static StructureStart loadStaticStart(StructureStart start, PiecesContainer piecesContainer, CompoundTag nbt) {
		if (start.getStructure() instanceof CustomStructureData s)
			return s.forDeserialization(start.getStructure(), start.getChunkPos(), start.getReferences(), piecesContainer, nbt);
		return start;
	}
}
