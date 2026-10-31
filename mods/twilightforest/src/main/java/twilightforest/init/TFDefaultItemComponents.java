package twilightforest.init;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.component.Compostable;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import twilightforest.TwilightForestMod;

public final class TFDefaultItemComponents {
    private TFDefaultItemComponents() {}
    public static void modify(ModifyDefaultComponentsEvent event) {
        compost(event, TFBlocks.FALLEN_LEAVES.asItem(), 10);
        compost(event, TFBlocks.CANOPY_LEAVES.asItem(), 30);
        compost(event, TFBlocks.CLOVER_PATCH.asItem(), 30);
        compost(event, TFBlocks.DARK_LEAVES.asItem(), 30);
        compost(event, TFBlocks.FIDDLEHEAD.asItem(), 30);
        compost(event, TFBlocks.HEDGE.asItem(), 30);
        compost(event, TFBlocks.MANGROVE_LEAVES.asItem(), 30);
        compost(event, TFBlocks.MAYAPPLE.asItem(), 30);
        compost(event, TFBlocks.MINING_LEAVES.asItem(), 30);
        compost(event, TFBlocks.TWILIGHT_OAK_LEAVES.asItem(), 30);
        compost(event, TFBlocks.RAINBOW_OAK_LEAVES.asItem(), 30);
        compost(event, TFBlocks.ROOT_STRAND.asItem(), 30);
        compost(event, TFBlocks.SORTING_LEAVES.asItem(), 30);
        compost(event, TFBlocks.THORN_LEAVES.asItem(), 30);
        compost(event, TFBlocks.TIME_LEAVES.asItem(), 30);
        compost(event, TFBlocks.TRANSFORMATION_LEAVES.asItem(), 30);
        compost(event, TFBlocks.TWILIGHT_OAK_SAPLING.asItem(), 30);
        compost(event, TFBlocks.CANOPY_SAPLING.asItem(), 30);
        compost(event, TFBlocks.MANGROVE_SAPLING.asItem(), 30);
        compost(event, TFBlocks.DARKWOOD_SAPLING.asItem(), 30);
        compost(event, TFBlocks.RAINBOW_OAK_SAPLING.asItem(), 30);
        compost(event, TFItems.TORCHBERRIES, 30);
        compost(event, TFBlocks.BEANSTALK_LEAVES.asItem(), 50);
        compost(event, TFBlocks.MOSS_PATCH.asItem(), 50);
        compost(event, TFBlocks.ROOT_BLOCK.asItem(), 50);
        compost(event, TFBlocks.THORN_ROSE.asItem(), 50);
        compost(event, TFBlocks.TROLLVIDR.asItem(), 50);
        compost(event, TFBlocks.HOLLOW_OAK_SAPLING.asItem(), 50);
        compost(event, TFBlocks.TIME_SAPLING.asItem(), 50);
        compost(event, TFBlocks.TRANSFORMATION_SAPLING.asItem(), 50);
        compost(event, TFBlocks.MINING_SAPLING.asItem(), 50);
        compost(event, TFBlocks.SORTING_SAPLING.asItem(), 50);
        compost(event, TFBlocks.TORCHBERRY_PLANT.asItem(), 50);
        compost(event, TFItems.LIVEROOT, 50);
        compost(event, TFBlocks.HUGE_MUSHGLOOM_STEM.asItem(), 65);
        compost(event, TFBlocks.HUGE_WATER_LILY.asItem(), 65);
        compost(event, TFBlocks.LIVEROOT_BLOCK.asItem(), 65);
        compost(event, TFBlocks.MUSHGLOOM.asItem(), 65);
        compost(event, TFBlocks.UBEROUS_SOIL.asItem(), 65);
        compost(event, TFBlocks.HUGE_STALK.asItem(), 65);
        compost(event, TFBlocks.UNRIPE_TROLLBER.asItem(), 65);
        compost(event, TFBlocks.TROLLBER.asItem(), 65);
        compost(event, TFItems.MAZE_WAFER, 65);
        compost(event, TFBlocks.HUGE_LILY_PAD.asItem(), 85);
        compost(event, TFBlocks.HUGE_MUSHGLOOM.asItem(), 85);
        compost(event, TFItems.EXPERIMENT_115, 85);
        compost(event, TFItems.MAGIC_BEANS, 85);
        compost(event, TFBlocks.RASPBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.BLUEBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.BLACKBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.MALOBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.BLIGHTBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.DUSKBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.SKYBERRY_BUSH.asItem(), 50);
        compost(event, TFBlocks.STINGBERRY_BUSH.asItem(), 50);
        compost(event, TFItems.RASPBERRY.get(), 30);
        compost(event, TFItems.BLUEBERRY.get(), 30);
        compost(event, TFItems.BLACKBERRY.get(), 30);
        compost(event, TFItems.MALOBERRY.get(), 30);
        compost(event, TFItems.BLIGHTBERRY.get(), 30);
        compost(event, TFItems.DUSKBERRY.get(), 30);
        compost(event, TFItems.SKYBERRY.get(), 30);
        compost(event, TFItems.STINGBERRY.get(), 30);
        TFFuelComponents.modify(event);
    }
    private static void compost(ModifyDefaultComponentsEvent event, ItemLike item, int chance) {
        event.modify(item, (components, context, key) -> components.set(DataComponents.COMPOSTABLE,
            new Compostable(ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, TwilightForestMod.prefix("compostable/legacy_"+chance)))));
    }
}
