package twilightforest.world.components.carver;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;

/** Keeps the TF 26.1.2 carving configuration, including its serialized field names. */
public record TFCaveConfiguration(float probability, HeightProvider y, FloatProvider yScale,
        VerticalAnchor lavaLevel, DebugSettings debugSettings, HolderSet<Block> replaceable,
        FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
    public static final Codec<TFCaveConfiguration> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.floatRange(0, 1).fieldOf("probability").forGetter(TFCaveConfiguration::probability),
        HeightProvider.CODEC.fieldOf("y").forGetter(TFCaveConfiguration::y),
        FloatProviders.CODEC.fieldOf("yScale").forGetter(TFCaveConfiguration::yScale),
        VerticalAnchor.CODEC.fieldOf("lava_level").forGetter(TFCaveConfiguration::lavaLevel),
        DebugSettings.CODEC.optionalFieldOf("debug_settings", DebugSettings.DEFAULT).forGetter(TFCaveConfiguration::debugSettings),
        RegistryCodecs.holderSet(Registries.BLOCK).fieldOf("replaceable").forGetter(TFCaveConfiguration::replaceable),
        FloatProviders.CODEC.fieldOf("horizontal_radius_multiplier").forGetter(TFCaveConfiguration::horizontalRadiusMultiplier),
        FloatProviders.CODEC.fieldOf("vertical_radius_multiplier").forGetter(TFCaveConfiguration::verticalRadiusMultiplier),
        FloatProviders.codec(-1, 1).fieldOf("floor_level").forGetter(TFCaveConfiguration::floorLevel)
    ).apply(i, TFCaveConfiguration::new));

    public TFCaveConfiguration(float probability, HeightProvider y, FloatProvider yScale, VerticalAnchor lavaLevel,
            HolderSet<Block> replaceable, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
        this(probability, y, yScale, lavaLevel, DebugSettings.DEFAULT, replaceable, horizontalRadiusMultiplier, verticalRadiusMultiplier, floorLevel);
    }

    public record DebugSettings(boolean debugMode, net.minecraft.world.level.block.state.BlockState airState,
            net.minecraft.world.level.block.state.BlockState waterState, net.minecraft.world.level.block.state.BlockState lavaState,
            net.minecraft.world.level.block.state.BlockState barrierState) {
        private static final net.minecraft.world.level.block.state.BlockState DEFAULT_AIR = net.minecraft.world.level.block.Blocks.ACACIA_BUTTON.defaultBlockState();
        public static final DebugSettings DEFAULT = new DebugSettings(false, DEFAULT_AIR,
            net.minecraft.world.level.block.Blocks.CANDLE.defaultBlockState(),
            net.minecraft.world.level.block.Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.ORANGE).defaultBlockState(),
            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
        // The original codec defaulted all omitted state fields to its air state.
        public static final Codec<DebugSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("debug_mode", false).forGetter(DebugSettings::debugMode),
            net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("air_state", DEFAULT_AIR).forGetter(DebugSettings::airState),
            net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("water_state", DEFAULT_AIR).forGetter(DebugSettings::waterState),
            net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("lava_state", DEFAULT_AIR).forGetter(DebugSettings::lavaState),
            net.minecraft.world.level.block.state.BlockState.CODEC.optionalFieldOf("barrier_state", DEFAULT_AIR).forGetter(DebugSettings::barrierState)
        ).apply(i, DebugSettings::new));
    }
}
