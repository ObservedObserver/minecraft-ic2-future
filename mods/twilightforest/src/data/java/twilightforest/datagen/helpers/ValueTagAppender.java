package twilightforest.datagen.helpers;

import java.util.function.Function;
import net.minecraft.data.tags.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;

/** Maps registered values to the keys required by the native tag writer. */
public final class ValueTagAppender<T> implements TagAppender<T> {
    private final TagAppender<T> output;
    private final Function<T, ResourceKey<T>> key;
    public ValueTagAppender(TagAppender<T> output, Function<T,ResourceKey<T>> key) { this.output=output; this.key=key; }
    public ValueTagAppender<T> add(T value) { this.output.add(this.key.apply(value)); return this; }
    @SafeVarargs public final ValueTagAppender<T> add(T... values) { for (T value:values) add(value); return this; }
    public ValueTagAppender<T> addOptional(T value) { this.output.addOptional(this.key.apply(value)); return this; }
    public ValueTagAppender<T> remove(T value) { this.output.remove(this.key.apply(value)); return this; }
    @SafeVarargs public final ValueTagAppender<T> remove(T... values) { for (T value:values) remove(value); return this; }
    @Override public ValueTagAppender<T> add(ResourceKey<T> key) { this.output.add(key); return this; }
    @Override public ValueTagAppender<T> addOptional(ResourceKey<T> key) { this.output.addOptional(key); return this; }
    @Override public ValueTagAppender<T> addTag(TagKey<T> tag) { this.output.addTag(tag); return this; }
    @SafeVarargs @Override public final ValueTagAppender<T> addTags(TagKey<T>... tags) { for(var tag:tags) addTag(tag); return this; }
    @Override public ValueTagAppender<T> addOptionalTag(TagKey<T> tag) { this.output.addOptionalTag(tag); return this; }
    @Override public ValueTagAppender<T> add(TagEntry entry) { this.output.add(entry); return this; }
    @Override public ValueTagAppender<T> replace(boolean value) { this.output.replace(value); return this; }
    @Override public ValueTagAppender<T> remove(ResourceKey<T> key) { this.output.remove(key); return this; }
    @Override public ValueTagAppender<T> remove(TagKey<T> tag) { this.output.remove(tag); return this; }
}
