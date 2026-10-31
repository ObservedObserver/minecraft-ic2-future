package twilightforest.datagen.data.recipes;

/** Registry bootstrap used by data generation; recipe output also registers advancements. */
public final class CraftingGeneratorRunner {
    private CraftingGeneratorRunner() {}
    public static net.minecraft.core.registries.MultiRegistryBootstrap bootstrap() {
        return net.minecraft.data.recipes.RecipeProvider.asBootstrap(CraftingGenerator::new);
    }
}
