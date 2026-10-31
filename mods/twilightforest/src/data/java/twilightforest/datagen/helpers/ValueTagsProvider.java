package twilightforest.datagen.helpers;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public abstract class ValueTagsProvider<T> extends TagsProvider<T> {
    private final Function<T,ResourceKey<T>> key;
    protected ValueTagsProvider(PackOutput output, ResourceKey<? extends Registry<T>> registry,
        CompletableFuture<HolderLookup.Provider> lookup, Function<T,ResourceKey<T>> key, String modId) {
        super(output,registry,lookup,modId); this.key=key;
    }
    @Override protected ValueTagAppender<T> tag(TagKey<T> tag) { return new ValueTagAppender<>(super.tag(tag),this.key); }
}
