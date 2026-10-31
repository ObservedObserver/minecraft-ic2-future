package twilightforest.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tamaized.beanification.junit.MockitoFixer;
import twilightforest.entity.ProtectionBox;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoFixer.class)
class ProtectionBoxRegressionTests {
    @Test void clientBoxesGetDistinctNonzeroIdsWithoutReplacingLiveEntities() {
        var level = mock(ClientLevel.class);
        var existing = mock(Entity.class);
        Map<Integer, Entity> entities = new HashMap<>();
        var occupiedId = new AtomicInteger();
        when(level.entitiesForRendering()).thenReturn(List.of());
        when(level.getEntity(anyInt())).thenAnswer(call -> {
            int id = call.getArgument(0);
            if (occupiedId.compareAndSet(0, id)) entities.put(id, existing);
            return entities.get(id);
        });
        doAnswer(call -> {
            Entity entity = call.getArgument(0);
            assertTrue(entity.getId() < 0);
            assertNull(entities.putIfAbsent(entity.getId(), entity), "Must never replace a live entity");
            return null;
        }).when(level).addEntity(any());
        try (var boxes = mockConstruction(ProtectionBox.class, (box, context) -> {
            doCallRealMethod().when(box).getId();
            doCallRealMethod().when(box).setId(anyInt());
            assertThrows(IllegalStateException.class, box::getId);
        })) {
            AreaProtectionPacket.addProtectionBox(level, new BoundingBox(0, 0, 0, 4, 4, 4));
            AreaProtectionPacket.addProtectionBox(level, new BoundingBox(10, 0, 0, 14, 4, 4));
            assertEquals(2, boxes.constructed().size());
            assertNotEquals(boxes.constructed().get(0).getId(), boxes.constructed().get(1).getId());
            assertSame(existing, entities.get(occupiedId.get()));
        }
    }

    @Test void repeatPacketRefreshesTheMatchingLiveBoxInsteadOfAddingAnother() {
        var level = mock(ClientLevel.class);
        var bounds = new BoundingBox(-20, 1, -20, -10, 8, -10);
        var box = mock(ProtectionBox.class);
        box.lifeTime = 12;
        when(box.matches(bounds)).thenReturn(true);
        when(level.entitiesForRendering()).thenReturn(List.of(box));
        AreaProtectionPacket.addProtectionBox(level, bounds);
        verify(box).resetLifetime();
        verify(level, never()).addEntity(any());
        verify(level, never()).getEntity(anyInt());
    }
}
