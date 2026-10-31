package twilightforest.world.components.carver;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Local compatibility subset of Minecraft 26.1.2 ImprovedNoise.
 * Keeps the double-precision sign test used by existing TF dirt cave walls.
 * Source: the official mapped 26.1.2 client; retained for this personal local port.
 */
public final class LegacyDirtNoise {
    private final byte[] p;
    public final double xo, yo, zo;
    public LegacyDirtNoise(RandomSource random) {
        this.xo = random.nextDouble() * 256.0;
        this.yo = random.nextDouble() * 256.0;
        this.zo = random.nextDouble() * 256.0;
        this.p = new byte[256];

        for (int i = 0; i < 256; i++) {
            this.p[i] = (byte)i;
        }

        for (int i = 0; i < 256; i++) {
            int offset = random.nextInt(256 - i);
            byte tmp = this.p[i];
            this.p[i] = this.p[i + offset];
            this.p[i + offset] = tmp;
        }
    }
    public double noise(double x, double y, double z) {
        x += xo; y += yo; z += zo;
        int ix = Mth.floor(x), iy = Mth.floor(y), iz = Mth.floor(z);
        return sampleAndLerp(ix, iy, iz, x - ix, y - iy, z - iz, y - iy);
    }
    private static final int[][] GRADIENT = {
        {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
        {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
        {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1},
        {1,1,0},{0,-1,1},{-1,1,0},{0,-1,-1}
    };
    private static double gradDot(int hash, double x, double y, double z) {
        int[] gradient = GRADIENT[hash & 15];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }
    private static double smoothstep(double x) { return x * x * x * (x * (x * 6.0 - 15.0) + 10.0); }
    private int p(int x) { return p[x & 255] & 255; }
    private double sampleAndLerp(int x, int y, int z, double xr, double yr, double zr, double yrOriginal) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);
        double d000 = gradDot(this.p(xy00 + z), xr, yr, zr);
        double d100 = gradDot(this.p(xy10 + z), xr - 1.0, yr, zr);
        double d010 = gradDot(this.p(xy01 + z), xr, yr - 1.0, zr);
        double d110 = gradDot(this.p(xy11 + z), xr - 1.0, yr - 1.0, zr);
        double d001 = gradDot(this.p(xy00 + z + 1), xr, yr, zr - 1.0);
        double d101 = gradDot(this.p(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
        double d011 = gradDot(this.p(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
        double d111 = gradDot(this.p(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);
        double xAlpha = smoothstep(xr);
        double yAlpha = smoothstep(yrOriginal);
        double zAlpha = smoothstep(zr);
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }
}
