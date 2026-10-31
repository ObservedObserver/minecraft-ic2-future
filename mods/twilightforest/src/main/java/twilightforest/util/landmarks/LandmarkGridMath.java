package twilightforest.util.landmarks;

import net.minecraft.core.BlockPos;

/** Seed-independent landmark center geometry retained from LegacyLandmarkPlacements. */
public final class LandmarkGridMath {
    private LandmarkGridMath() { }
    public static BlockPos getNearestCenterXZ(int chunkX, int chunkZ) { return getNearestCenterXZ(chunkX, chunkZ, 0); }
	public static BlockPos getNearestCenterXZ(int chunkX, int chunkZ, int height) {
		// generate random number for the whole biome area
		int regionX = (chunkX + 8) >> 4;
		int regionZ = (chunkZ + 8) >> 4;

		long seed = regionX * 3129871L ^ regionZ * 116129781L;
		seed = seed * seed * 42317861L + seed * 7L;

		int num0 = (int) (seed >> 12 & 3L);
		int num1 = (int) (seed >> 15 & 3L);
		int num2 = (int) (seed >> 18 & 3L);
		int num3 = (int) (seed >> 21 & 3L);

		// slightly randomize center of biome (+/- 3)
		int centerX = 8 + num0 - num1;
		int centerZ = 8 + num2 - num3;

		// centers are offset strangely depending on +/-
		int ccz;
		if (regionZ >= 0) {
			ccz = (regionZ * 16 + centerZ - 8) * 16 + 8;
		} else {
			ccz = (regionZ * 16 + (16 - centerZ) - 8) * 16 + 9;
		}

		int ccx;
		if (regionX >= 0) {
			ccx = (regionX * 16 + centerX - 8) * 16 + 8;
		} else {
			ccx = (regionX * 16 + (16 - centerX) - 8) * 16 + 9;
		}

		return new BlockPos(ccx, height, ccz);
	}
}
