package validation;

import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import com.mojang.datafixers.util.Pair;
import org.junit.jupiter.api.Test;
import twilightforest.init.TFFuelComponents;
import static org.junit.jupiter.api.Assertions.*;

class FuelComponentBindingTest {
    @Test void legacyFuelBindingsEncodeThroughNativeCookingComponentCodec() {
        for(var entry:TFFuelComponents.BURN_TIMES.entrySet()) {
            var fuel=new CookingFuel(TFFuelComponents.provider(entry.getValue()),ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER);
            var encoded=CookingFuel.CODEC.encodeStart(JsonOps.INSTANCE,fuel).getOrThrow();
            assertEquals(fuel,CookingFuel.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow(),entry.getKey());
        }
    }
    @Test void registrationDoesNotChangeAnyVanillaOrOtherRegisteredItem() {
        var matching=new ArrayList<Pair<ModifyDefaultComponentsEvent.ItemWithComponentsPredicate,ModifyDefaultComponentsEvent.Initializer>>();
        TFFuelComponents.modify(new ModifyDefaultComponentsEvent(new HashMap<>(),matching));
        assertEquals(1,matching.size());
        int checked=0;
        for(var item:BuiltInRegistries.ITEM) {
            if(!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("twilightforest")) {
                assertFalse(matching.getFirst().getFirst().test(item,DataComponentMap.EMPTY));checked++;
            }
        }
        assertTrue(checked>100);
    }
}
