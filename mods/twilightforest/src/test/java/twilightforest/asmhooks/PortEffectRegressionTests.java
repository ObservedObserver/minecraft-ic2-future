package twilightforest.asmhooks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tamaized.beanification.junit.MockitoFixer;
import twilightforest.init.custom.TravellersModifiersManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoFixer.class)
class PortEffectRegressionTests {
    @Test void ordinaryPlayersRetainSlimeBounce() {
        var entity = mock(Entity.class);
        when(entity.getDeltaMovement()).thenReturn(new Vec3(0.4, -0.04, 0.2));
        try (var modifiers = mockStatic(TravellersModifiersManager.class)) {
            modifiers.when(() -> TravellersModifiersManager.isModifierActive(entity,
                TravellersModifiersManager.UNRESTRAINED_MODIFIER)).thenReturn(false);
            assertEquals(1.0, BlockHooks.filterSlimeRestitution(1.0, entity, Blocks.SLIME_BLOCK.defaultBlockState()));
        }
    }
    @Test void unrestrainedSuppressesResidualSlimeBounceButRetainsRealFallsAndBeds() {
        var entity = mock(Entity.class);
        try (var modifiers = mockStatic(TravellersModifiersManager.class)) {
            modifiers.when(() -> TravellersModifiersManager.isModifierActive(entity,
                TravellersModifiersManager.UNRESTRAINED_MODIFIER)).thenReturn(true);
            when(entity.getDeltaMovement()).thenReturn(new Vec3(0.4, -0.04, 0.2));
            assertEquals(0.0, BlockHooks.filterSlimeRestitution(1.0, entity, Blocks.SLIME_BLOCK.defaultBlockState()));
            assertEquals(0.66, BlockHooks.filterSlimeRestitution(0.66, entity, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState()));
            when(entity.getDeltaMovement()).thenReturn(new Vec3(0.4, -0.08, 0.2));
            assertEquals(1.0, BlockHooks.filterSlimeRestitution(1.0, entity, Blocks.SLIME_BLOCK.defaultBlockState()));
            when(entity.getDeltaMovement()).thenReturn(new Vec3(0.4, -0.5, 0.2));
            assertEquals(1.0, BlockHooks.filterSlimeRestitution(1.0, entity, Blocks.SLIME_BLOCK.defaultBlockState()));
        }
    }

    @Test void clothVisibilityDiscountTracksZeroThroughFourArmorComponents() throws Exception {
        var dependency = ArmorHooks.class.getDeclaredField("armorUtil");
        dependency.setAccessible(true);
        var previous = dependency.get(null);
        dependency.set(null, new twilightforest.util.ArmorUtil());
        try {
            var entity = mock(net.minecraft.world.entity.LivingEntity.class);
            var slots = java.util.List.of(net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET);
            for (int covered = 0; covered <= 4; covered++) {
                for (int i = 0; i < slots.size(); i++) {
                    var stack = new net.minecraft.world.item.ItemStack(net.minecraft.core.Holder.direct(net.minecraft.world.item.Items.IRON_CHESTPLATE), 1);
                    if (i < covered) stack.set(twilightforest.init.TFDataComponents.EMPERORS_CLOTH.get(), net.minecraft.util.Unit.INSTANCE);
                    when(entity.getItemBySlot(slots.get(i))).thenReturn(stack);
                }
                assertEquals(1.0F - covered * 0.25F, ArmorHooks.modifyArmorVisibility(1.0F, entity));
            }
        } finally {
            dependency.set(null, previous);
        }
    }
}
