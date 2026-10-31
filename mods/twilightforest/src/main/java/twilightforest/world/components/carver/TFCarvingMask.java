package twilightforest.world.components.carver;

import java.util.BitSet;
import net.minecraft.world.level.chunk.CarvingMask;

/** Per-generation ownership shared by TF's immediate carving and native deferred carving. */
public final class TFCarvingMask extends CarvingMask {
    private final BitSet claimed = new BitSet();
    private final int height;
    public TFCarvingMask(int minY, int maxY) { super(minY, maxY); height = maxY - minY + 1; }

    public boolean claim(int x, int y, int z) {
        int index = y - minY() + (z + (x << 4)) * height;
        if (claimed.get(index)) return false;
        claimed.set(index);
        return true;
    }

    @Override
    public void carve(int x, int y, int z) {
        if (claim(x, y, z)) super.carve(x, y, z);
    }
}
