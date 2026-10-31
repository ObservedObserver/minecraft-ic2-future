package ic2.compat;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class SoundCompat {
  private SoundCompat() {}

  public static void notify(
      Player player, SoundEvent sound, SoundSource source, float volume, float pitch) {
    if (player instanceof ServerPlayer serverPlayer) {
      serverPlayer.connection.send(
          new ClientboundSoundPacket(
              Holder.direct(sound),
              source,
              player.getX(),
              player.getY(),
              player.getZ(),
              volume,
              pitch,
              player.getRandom().nextLong()));
    } else if (player.level().isClientSide()) {
      player
          .level()
          .playLocalSound(
              player.getX(), player.getY(), player.getZ(), sound, source, volume, pitch, false);
    }
  }
}
