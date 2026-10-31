package twilightforest.datagen.data.worldgen.structures;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import twilightforest.TFRegistries;
import twilightforest.init.TFEntities;
import twilightforest.init.TFMapDecorations;
import twilightforest.init.custom.StructureSpeleothemConfigs;
import twilightforest.tags.TFBiomeTags;
import twilightforest.world.components.structures.type.HollowHillStructure;
import twilightforest.world.components.structures.util.ControlledSpawns;
import twilightforest.world.components.structures.util.DecorationClearance;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class HollowHillStructureGenerator {

    public static HollowHillStructure buildSmallHillConfig(BootstrapContext<Structure> context) {
        return new HollowHillStructure(
            1,
            ControlledSpawns.ControlledSpawningConfig.create(List.of(WeightedList.<MobSpawnSettings.SpawnerData>builder()
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.SPIDER, net.minecraft.util.valueproviders.UniformInt.of(4, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.ZOMBIE, net.minecraft.util.valueproviders.UniformInt.of(4, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.REDCAP.get(), net.minecraft.util.valueproviders.UniformInt.of(4, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.SWARM_SPIDER.get(), net.minecraft.util.valueproviders.UniformInt.of(4, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.KOBOLD.get(), net.minecraft.util.valueproviders.UniformInt.of(4, 8)), 10)
                .build()
            ), WeightedList.of(), WeightedList.of()),
            context.lookup(TFRegistries.Keys.STRUCTURE_SPELEOTHEM_SETTINGS).getOrThrow(StructureSpeleothemConfigs.SMALL_HILL),
            Optional.of(new DecorationClearance.DecorationConfig(1, true, false, false)),
            true, Optional.of(TFMapDecorations.SMALL_HOLLOW_HILL),
            new Structure.StructureSettings(
                context.lookup(Registries.BIOME).getOrThrow(TFBiomeTags.VALID_HOLLOW_HILL_BIOMES),
                Arrays.stream(MobCategory.values()).collect(Collectors.<MobCategory, MobCategory, StructureSpawnOverride>toMap(category -> category, category -> new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE, WeightedList.<MobSpawnSettings.SpawnerData>builder().build()))), // Landmarks have Controlled Mob spawning
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE
            )
        );
    }

    public static HollowHillStructure buildMediumHillConfig(BootstrapContext<Structure> context) {
        return new HollowHillStructure(
            2,
            ControlledSpawns.ControlledSpawningConfig.create(List.of(WeightedList.<MobSpawnSettings.SpawnerData>builder()
                .add(new MobSpawnSettings.SpawnerData(TFEntities.REDCAP.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.REDCAP_SAPPER.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 2)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.KOBOLD.get(), net.minecraft.util.valueproviders.UniformInt.of(2, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.SKELETON, net.minecraft.util.valueproviders.UniformInt.of(2, 3)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.SWARM_SPIDER.get(), net.minecraft.util.valueproviders.UniformInt.of(2, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.SPIDER, net.minecraft.util.valueproviders.UniformInt.of(1, 3)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.CREEPER, net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 5)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.FIRE_BEETLE.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 1)), 5)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.SLIME_BEETLE.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 1)), 5)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.WITCH, net.minecraft.util.valueproviders.UniformInt.of(1, 1)), 1)
                .build()
            ), WeightedList.of(), WeightedList.of()),
            context.lookup(TFRegistries.Keys.STRUCTURE_SPELEOTHEM_SETTINGS).getOrThrow(StructureSpeleothemConfigs.MEDIUM_HILL),
            Optional.of(new DecorationClearance.DecorationConfig(2, true, false, false)),
            true, Optional.of(TFMapDecorations.MEDIUM_HOLLOW_HILL),
            new Structure.StructureSettings(
                context.lookup(Registries.BIOME).getOrThrow(TFBiomeTags.VALID_HOLLOW_HILL_BIOMES),
                Arrays.stream(MobCategory.values()).collect(Collectors.<MobCategory, MobCategory, StructureSpawnOverride>toMap(category -> category, category -> new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE, WeightedList.<MobSpawnSettings.SpawnerData>builder().build()))), // Landmarks have Controlled Mob spawning
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE
            )
        );
    }

    public static HollowHillStructure buildLargeHillConfig(BootstrapContext<Structure> context) {
        return new HollowHillStructure(
            3,
            ControlledSpawns.ControlledSpawningConfig.firstIndexMonsters(WeightedList.<MobSpawnSettings.SpawnerData>builder()
                .add(new MobSpawnSettings.SpawnerData(TFEntities.REDCAP.get(), net.minecraft.util.valueproviders.UniformInt.of(2, 4)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.REDCAP_SAPPER.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 2)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.SKELETON, net.minecraft.util.valueproviders.UniformInt.of(2, 3)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.CAVE_SPIDER, net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.CREEPER, net.minecraft.util.valueproviders.UniformInt.of(1, 1)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.ENDERMAN, net.minecraft.util.valueproviders.UniformInt.of(1, 1)), 1)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.WRAITH.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 2)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.FIRE_BEETLE.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.SLIME_BEETLE.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 10)
                .add(new MobSpawnSettings.SpawnerData(TFEntities.PINCH_BEETLE.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 10)
                .add(new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityTypes.WITCH, net.minecraft.util.valueproviders.UniformInt.of(1, 1)), 1)
                .build()
            ),
            context.lookup(TFRegistries.Keys.STRUCTURE_SPELEOTHEM_SETTINGS).getOrThrow(StructureSpeleothemConfigs.LARGE_HILL),
            Optional.of(new DecorationClearance.DecorationConfig(3, true, false, false)),
            true, Optional.of(TFMapDecorations.LARGE_HOLLOW_HILL),
            new Structure.StructureSettings(
                context.lookup(Registries.BIOME).getOrThrow(TFBiomeTags.VALID_HOLLOW_HILL_BIOMES),
                Arrays.stream(MobCategory.values()).collect(Collectors.<MobCategory, MobCategory, StructureSpawnOverride>toMap(category -> category, category -> new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE, WeightedList.<MobSpawnSettings.SpawnerData>builder().build()))), // Landmarks have Controlled Mob spawning
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                TerrainAdjustment.NONE
            )
        );
    }

}
