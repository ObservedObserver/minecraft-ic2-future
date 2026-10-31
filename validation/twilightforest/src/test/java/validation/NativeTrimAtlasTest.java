package validation;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeTrimAtlasTest {
    @Test void itemTrimPermutationsUseNativePaletteLookupAndNeoForgeSuffixes() throws Exception {
        Path root=Path.of(System.getProperty("twilightforest.projectRoot"));
        var atlas=JsonParser.parseString(Files.readString(root.resolve("src/generated/resources/assets/minecraft/atlases/items.json"))).getAsJsonObject();
        var source=atlas.getAsJsonArray("sources").asList().stream().filter(x -> x.getAsJsonObject().get("type").getAsString().equals("minecraft:paletted_permutations")).findFirst().orElseThrow();
        var decoded=PalettedPermutations.MAP_CODEC.codec().parse(JsonOps.INSTANCE,source).getOrThrow();
        assertEquals(Identifier.withDefaultNamespace("trim_base"),decoded.paletteKey());
        assertEquals(4,decoded.textures().size());assertEquals(6,decoded.permutations().size());
        for(var entry:decoded.permutations().entrySet()) {
            // Actual NeoForge TrimmedArmorModel derives sprite suffixes from the palette path.
            assertEquals(entry.getValue().getPath().replaceFirst("trim/", ""),entry.getKey());
            var palette=root.resolve("src/main/resources/assets/"+entry.getValue().getNamespace()+"/textures/palettes/"+entry.getValue().getPath()+".png");
            assertTrue(Files.isRegularFile(palette),palette.toString());
        }
    }
    @Test void wornTrimMaterialsUseNativePalettesWithoutTheRemovedAtlas() {
        var root=Path.of(System.getProperty("twilightforest.projectRoot"));
        assertFalse(Files.exists(root.resolve("src/generated/resources/assets/minecraft/atlases/armor_trims.json")));
    }
}
