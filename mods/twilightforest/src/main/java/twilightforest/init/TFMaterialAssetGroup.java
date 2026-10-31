package twilightforest.init;

import net.minecraft.resources.Identifier;

/** Native 26.3 palette identifiers for the existing TF trim colors. */
public final class TFMaterialAssetGroup {
    private TFMaterialAssetGroup() { }
    public static final Identifier IRONWOOD = palette("ironwood");
    public static final Identifier STEELEAF = palette("steeleaf");
    public static final Identifier KNIGHTMETAL = palette("knightmetal");
    public static final Identifier FIERY = palette("fiery");
    public static final Identifier NAGA_SCALE = palette("naga_scale");
    public static final Identifier CARMINITE = palette("carminite");
    private static Identifier palette(String name) { return Identifier.fromNamespaceAndPath("twilightforest", "trim/" + name); }
}
