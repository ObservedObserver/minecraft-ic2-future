package validation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrimPaletteResourceTest {
    private static final String[] MATERIALS = {"ironwood","steeleaf","knightmetal","fiery","naga_scale","carminite"};
    @Test
    void allSixTrimMaterialsDecodeWithNativePaletteIdentifiers() throws Exception {
        var root = Path.of(System.getProperty("twilightforest.projectRoot"));
        for (String name : MATERIALS) {
            var json = JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/data/twilightforest/trim_material/" + name + ".json")));
            var material = TrimMaterial.DIRECT_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            assertEquals("twilightforest:trim/" + name, material.paletteId().toString());
            assertNotNull(material.description().getStyle().getColor());
            var encoded = TrimMaterial.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, material).getOrThrow();
            assertEquals(material, TrimMaterial.DIRECT_CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow());
        }
    }
    @Test
    void newNativePalettePathsKeepTheExactExistingColorPixels() throws Exception {
        var textures = Path.of(System.getProperty("twilightforest.projectRoot")).resolve("src/main/resources/assets/twilightforest/textures");
        for (String name : MATERIALS) {
            assertArrayEquals(Files.readAllBytes(textures.resolve("trims/color_palettes/" + name + ".png")),
                Files.readAllBytes(textures.resolve("palettes/trim/" + name + ".png")));
        }
    }
}
