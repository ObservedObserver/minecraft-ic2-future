package twilightforest.datagen.helpers;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;

/** Restore only removed vanilla tags used by this mod, without overriding 26.3 definitions. */
public final class LegacyVanillaTags {
    private LegacyVanillaTags() {}
    public static <T> void append(ResourceKey<? extends Registry<T>> registry, String type, BiConsumer<TagKey<T>,TagEntry> output) {
        try (var stream=Objects.requireNonNull(LegacyVanillaTags.class.getResourceAsStream("/twilightforest/datagen/legacy_vanilla_tags.json"));
             var reader=new InputStreamReader(stream,StandardCharsets.UTF_8)) {
            var tags=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject(type);
            for(var tag:tags.entrySet()) {
                var key=TagKey.create(registry,Identifier.withDefaultNamespace(tag.getKey()));
                var file=TagFile.CODEC.parse(JsonOps.INSTANCE,tag.getValue()).getOrThrow();
                if(file.replace() || !file.remove().isEmpty()) throw new IllegalStateException("Expected additive vanilla compatibility tag: "+key);
                for(var entry:file.entries()) output.accept(key,entry);
            }
        } catch(java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
}
