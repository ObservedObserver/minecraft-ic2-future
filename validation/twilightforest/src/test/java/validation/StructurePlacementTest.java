package validation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.junit.jupiter.api.Test;
import twilightforest.util.landmarks.LandmarkGridMath;
import twilightforest.world.components.structures.placements.AvoidLandmarkGridPlacement;
import static org.junit.jupiter.api.Assertions.*;

class StructurePlacementTest {
    @Test
    void retainedLandmarkGridKeepsPositiveNegativeAndRegionBoundaryCenters() {
        int[][] cases = {{0,0,8,8}, {16,16,264,264}, {-16,-16,-247,-247}, {-9,0,-247,8}, {8,0,280,56}, {100,-100,1544,-1495}, {-1024,2080,-16375,33288}};
        for (int[] c : cases) assertEquals(new BlockPos(c[2], 73, c[3]), LandmarkGridMath.getNearestCenterXZ(c[0], c[1], 73));
    }

    @Test
    void avoidGridUsesTheNativeRandomSpreadCandidatesAcrossSeedsAndNegativeRegions() {
        for (RandomSpreadType type : RandomSpreadType.values()) {
            var nativePlacement = new RandomSpreadStructurePlacement(7, 5, type, 1275623845);
            var tf = new AvoidLandmarkGridPlacement(7, 5, type, 1275623845, Optional.empty());
            for (long seed : new long[]{0, 87291, -391827498L}) for (int x : new int[]{-65,-16,-1,0,7,18,64}) for (int z : new int[]{-64,-7,0,6,63}) {
                assertEquals(nativePlacement.getPotentialStructureChunk(seed, x, z), tf.getPotentialStructureChunk(seed, x, z));
            }
        }
    }

    @Test
    void originalEightyBlockBufferRetainsChunkMinimumOffsetAsymmetry() {
        assertFalse(AvoidLandmarkGridPlacement.isOutsideLandmarkBuffer(new ChunkPos(0, 0)));
        assertFalse(AvoidLandmarkGridPlacement.isOutsideLandmarkBuffer(new ChunkPos(5, 0)));
        assertTrue(AvoidLandmarkGridPlacement.isOutsideLandmarkBuffer(new ChunkPos(6, 0)));
        assertTrue(AvoidLandmarkGridPlacement.isOutsideLandmarkBuffer(new ChunkPos(-5, 0)));
        assertFalse(AvoidLandmarkGridPlacement.isOutsideLandmarkBuffer(new ChunkPos(-16, -16)));
    }

    @Test
    void placementCodecRoundTripKeepsSpreadAndRejectsImpossibleSpacing() {
        var json = JsonParser.parseString("{\"salt\":1275623845,\"spacing\":7,\"separation\":5,\"spread_type\":\"triangular\",\"frequency\":0.8}");
        var placement = AvoidLandmarkGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertSame(AvoidLandmarkGridPlacement.CODEC, placement.codec());
        var encoded = AvoidLandmarkGridPlacement.CODEC.codec().encodeStart(JsonOps.INSTANCE, placement).getOrThrow();
        var decoded = AvoidLandmarkGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertEquals(placement.getPotentialStructureChunk(328, -72, 13), decoded.getPotentialStructureChunk(328, -72, 13));
        json.getAsJsonObject().addProperty("separation", 7);
        assertTrue(AvoidLandmarkGridPlacement.CODEC.codec().parse(JsonOps.INSTANCE, json).error().isPresent());
    }
}
