package twilightforest.datagen.data.worldgen;

import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.MobSpawnSettingsModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;

/** Retains explicit categories, spawn costs and initial generation probability. */
public final class TFSpawnSettingsBuilder extends MobSpawnSettings.Builder {
    private float probability=0.1F;
    public TFSpawnSettingsBuilder creatureGenerationProbability(float probability) { this.probability=probability; return this; }
    public TFSpawnSettingsBuilder addSpawn(MobCategory category,int weight,MobSpawnSettings.SpawnerData data) {
        super.addSpawn(data.type(),category,weight,data.count()); return this;
    }
    public TFSpawnSettingsBuilder addMobCharge(EntityType<?> type,double charge,double budget) {
        super.addMobSpawnCost(type,charge,budget); return this;
    }
    public EnvironmentAttributeMap attributes() {
        return EnvironmentAttributeMap.builder()
            .modify(EnvironmentAttributes.NATURAL_MOB_SPAWNS,MobSpawnSettingsModifier.overlay(),this.build())
            .set(EnvironmentAttributes.CREATURE_WORLD_GEN_SPAWN_PROBABILITY,this.probability).build();
    }
}
