package validation;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import twilightforest.world.components.placements.ChunkCenterModifier;
import static org.junit.jupiter.api.Assertions.*;

class ChunkCenterModifierTest {
    @Test
    void nativeConsumerPlacementKeepsNegativeChunkBoundariesAndY() {
        var modifier = ChunkCenterModifier.center();
        for (int x : new int[]{-33, -32, -17, -16, -1, 0, 15, 16, 31, 32}) {
            var positions = new ArrayList<BlockPos>();
            modifier.modify(null, RandomSource.create(1), new BlockPos(x, -12, -x), positions::add);
            assertEquals(java.util.List.of(new BlockPos(Math.floorDiv(x, 16) * 16 + 8, -12, Math.floorDiv(-x, 16) * 16 + 8)), positions);
        }
    }

    @Test
    void unitCodecAndPlacementDoNotAdvanceTheSharedRandomStream() {
        var modifier = ChunkCenterModifier.CODEC.codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        var random = RandomSource.create(872);
        modifier.modify(null, random, BlockPos.ZERO, pos -> { });
        assertEquals(RandomSource.create(872).nextLong(), random.nextLong());
        assertSame(ChunkCenterModifier.center(), modifier);
    }
}
