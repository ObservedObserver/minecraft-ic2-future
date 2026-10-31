package twilightforest.world.components.structures.placements;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tamaized.beanification.junit.MockitoFixer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoFixer.class)
class StructureLocateRegressionTests {
    @Test void nativeLocatorFindsAllAffectedStructureSetsAtPositiveAndNegativeCoordinates() throws Exception {
        for (String set : List.of("hollow_tree", "fallen_trunk", "camp")) {
            com.google.gson.JsonObject json;
            try (var input = getClass().getResourceAsStream("/data/twilightforest/worldgen/structure_set/" + set + ".json")) {
                assertNotNull(input);
                json = JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            }
            // Registry-backed exclusion holders are tested separately; this test exercises
            // the native locator's type dispatch, including instances decoded by our codec.
            var config = json.getAsJsonObject("placement").deepCopy();
            config.remove("exclusion_zone"); config.remove("avoid_additional_structures");
            var placement = AvoidLandmarkGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, config).getOrThrow();
            assertInstanceOf(AvoidLandmarkGridPlacement.class, placement);
            assertSame(AvoidLandmarkGridPlacement.CODEC, placement.codec());
            var dispatched = net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.CODEC.parse(JsonOps.INSTANCE, config).getOrThrow();
            assertInstanceOf(AvoidLandmarkGridPlacement.class, dispatched);
            var encoded = net.minecraft.world.level.levelgen.structure.placement.StructurePlacement.CODEC.encodeStart(JsonOps.INSTANCE, dispatched).getOrThrow();
            assertEquals("twilightforest:avoid_landmark_grid", encoded.getAsJsonObject().get("type").getAsString());
            for (var selection : json.getAsJsonArray("structures")) {
                String id = selection.getAsJsonObject().get("structure").getAsString();
                for (BlockPos origin : List.of(new BlockPos(128, 64, 512), new BlockPos(-513, 64, -129))) {
                    var level = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
                    when(level.getServer().getWorldGenSettings().options().generateStructures()).thenReturn(true);
                    var state = level.getChunkSource().getGeneratorState();
                    var structure = mock(Structure.class);
                    Holder<Structure> holder = Holder.direct(structure);
                    when(state.getPlacementsForStructure(holder)).thenReturn(List.of(placement));
                    when(state.getLevelSeed()).thenReturn(417L);
                    var target = placement.getPotentialStructureChunk(417L, origin.getX() >> 4, origin.getZ() >> 4);
                    when(level.structureManager().checkStructurePresence(target, structure, placement, false)).thenReturn(StructureCheckResult.START_PRESENT);
                    var generator = mock(ChunkGenerator.class, CALLS_REAL_METHODS);
                    var found = generator.findNearestMapStructure(level, HolderSet.direct(holder), origin, 0, false);
                    assertNotNull(found, id + " at " + origin);
                    assertEquals(placement.getLocatePos(target), found.getFirst());
                    assertSame(holder, found.getSecond());
                    verify(level.structureManager()).checkStructurePresence(target, structure, placement, false);
                }
            }
        }
    }

    @Test void nativeLocatorRespectsMissingStartsAndCreatesReferencesOnlyWhenRequested() {
        var placement = new AvoidLandmarkGridPlacement(7, 5, RandomSpreadType.LINEAR, 123, Optional.empty());
        var level = mock(ServerLevel.class, RETURNS_DEEP_STUBS);
        when(level.getServer().getWorldGenSettings().options().generateStructures()).thenReturn(true);
        var structure = mock(Structure.class);
        Holder<Structure> holder = Holder.direct(structure);
        when(level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder)).thenReturn(List.of(placement));
        var target = placement.getPotentialStructureChunk(0, 0, 0);
        var generator = mock(ChunkGenerator.class, CALLS_REAL_METHODS);
        when(level.structureManager().checkStructurePresence(target, structure, placement, false)).thenReturn(StructureCheckResult.START_NOT_PRESENT);
        assertNull(generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 0, false));
        when(level.structureManager().checkStructurePresence(target, structure, placement, true)).thenReturn(StructureCheckResult.CHUNK_LOAD_NEEDED);
        var chunk = level.getChunk(target.x(), target.z(), ChunkStatus.STRUCTURE_STARTS);
        var start = level.structureManager().getStartForStructure(structure, chunk);
        when(start.isValid()).thenReturn(true);
        when(start.canBeReferenced()).thenReturn(true);
        when(start.getChunkPos()).thenReturn(target);
        assertNotNull(generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 0, true));
        verify(level.structureManager()).addReference(start);
        when(start.canBeReferenced()).thenReturn(false);
        assertNull(generator.findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 0, true));
    }

    @Test void restoringNativeInheritanceRetainsFrequencyAndLandmarkExclusion() {
        var state = mock(ChunkGeneratorStructureState.class);
        when(state.getLevelSeed()).thenReturn(4321L);
        var always = new AvoidLandmarkGridPlacement(7, 5, RandomSpreadType.LINEAR, 123, Optional.empty());
        var never = new AvoidLandmarkGridPlacement(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT,
            0, 123, Optional.empty(), 7, 5, RandomSpreadType.LINEAR, Optional.empty());
        int allowed = 0, rejected = 0;
        for (int x = -128; x <= 128; x += 7) for (int z = -128; z <= 128; z += 7) {
            ChunkPos candidate = always.getPotentialStructureChunk(4321L, x, z);
            boolean outside = AvoidLandmarkGridPlacement.isOutsideLandmarkBuffer(candidate);
            assertEquals(outside, always.isStructureChunk(state, candidate.x(), candidate.z()));
            assertFalse(never.isStructureChunk(state, candidate.x(), candidate.z()));
            if (outside) allowed++; else rejected++;
        }
        assertTrue(allowed > 0 && rejected > 0);
    }

    @Test void nativeAndAdditionalStructureExclusionsStillRejectPlacement() {
        var state = mock(ChunkGeneratorStructureState.class);
        Holder<net.minecraft.world.level.levelgen.structure.StructureSet> primary = Holder.direct(mock(net.minecraft.world.level.levelgen.structure.StructureSet.class));
        Holder<net.minecraft.world.level.levelgen.structure.StructureSet> additional = Holder.direct(mock(net.minecraft.world.level.levelgen.structure.StructureSet.class));
        var avoid = new it.unimi.dsi.fastutil.objects.Object2IntArrayMap<Holder<net.minecraft.world.level.levelgen.structure.StructureSet>>();
        avoid.put(additional, 2);
        var placement = new AvoidLandmarkGridPlacement(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT,
            1, 123, Optional.of(new AbstractSpreadingStructurePlacement.ExclusionZone(primary, 1)), 7, 5, RandomSpreadType.LINEAR,
            Optional.of(new AvoidLandmarkGridPlacement.AvoidAdditionalStructures(avoid)));
        assertTrue(placement.applyInteractionsWithOtherStructures(state, -13, 27));
        when(state.hasStructureChunkInRange(primary, -13, 27, 1)).thenReturn(true);
        assertFalse(placement.applyInteractionsWithOtherStructures(state, -13, 27));
        when(state.hasStructureChunkInRange(primary, -13, 27, 1)).thenReturn(false);
        when(state.hasStructureChunkInRange(additional, -13, 27, 2)).thenReturn(true);
        assertFalse(placement.applyInteractionsWithOtherStructures(state, -13, 27));
    }

}
