package ic2.compat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.Level;

/** Explicit bridges for level state moved to the server's shared world data in 26.3. */
public final class WorldCompat {
  private WorldCompat() {}

  public static MinecraftServer server(Level level) {
    return level instanceof ServerLevel serverLevel ? serverLevel.getServer() : null;
  }

  public static void setDayTime(ServerLevel level, long ticks) {
    level
        .clockManager()
        .setTotalTicks(level.registryAccess().getOrThrow(WorldClocks.OVERWORLD), ticks);
  }

  public static void setWeather(
      ServerLevel level, int clearTicks, int weatherTicks, boolean raining, boolean thundering) {
    var weather = level.getWeatherData();
    weather.setClearWeatherTime(clearTicks);
    weather.setRainTime(weatherTicks);
    weather.setThunderTime(weatherTicks);
    weather.setRaining(raining);
    weather.setThundering(thundering);
  }
}
