package ic2.forge;

import ic2.core.event.EventHandlerClient;
import ic2.core.event.TickHandler;
import ic2.core.sound.DeferredSoundOps;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class ClientEventHandlerForge {
  @SubscribeEvent
  public void receiveRecipes(net.neoforged.neoforge.client.event.RecipesReceivedEvent event) {
    ic2.core.proxy.SideProxyClient.acceptRecipes(event.getRecipeMap());
  }

  @SubscribeEvent
  public void clearRecipes(
      net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
    ic2.core.proxy.SideProxyClient.acceptRecipes(net.minecraft.world.item.crafting.RecipeMap.EMPTY);
  }

  @SubscribeEvent
  public void onClientTickPre(ClientTickEvent.Pre event) {
    TickHandler.onClientTick();
  }

  @SubscribeEvent
  public void onClientTickPost(ClientTickEvent.Post event) {
    var player = net.minecraft.client.Minecraft.getInstance().player;
    if (player != null && !net.minecraft.client.Minecraft.getInstance().isPaused()) {
      ic2.compat.InventoryCompat.tickClient(player);
    }
    DeferredSoundOps.flush();
  }

  @SubscribeEvent
  public void onGuiCreate(ScreenEvent.Init.Post event) {
    EventHandlerClient.onGuiCreate(event.getScreen(), event.getListenersList(), event::addListener);
  }

  @SubscribeEvent
  public void onRenderHotBar(RenderGuiLayerEvent.Post event) {
    if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
      EventHandlerClient.onRenderHotBar(event.getGuiGraphics());
    }
  }

  @SubscribeEvent
  public void onSoundPlayed(PlaySoundEvent event) {
    SoundInstance sound = event.getSound();
    SoundInstance newSound = EventHandlerClient.onSoundPlayed(sound);
    if (newSound != sound) {
      event.setSound(newSound);
    }
  }

  @SubscribeEvent
  public void onDisconnect(PlayerEvent.PlayerLoggedOutEvent event) {
    EventHandlerClient.onDisconnect();
  }

  @SubscribeEvent
  public void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    // More reliable than PlayerLoggedOutEvent when leaving a dedicated server.
    EventHandlerClient.onDisconnect();
  }

  @SubscribeEvent
  public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity().level().isClientSide()) {
      EventHandlerClient.onClientPlayerJoin(event.getEntity());
    }
  }

  @SubscribeEvent
  public void onWorldLoad(LevelEvent.Load event) {
    Level world = (Level) event.getLevel();
    if (!world.isClientSide()) {
      return;
    }

    TickHandler.requestSingleWorldTick(
        world,
        loadedWorld -> {
          if (net.minecraft.client.Minecraft.getInstance().player != null
              && net.minecraft.client.Minecraft.getInstance().player.level() == loadedWorld) {
            EventHandlerClient.onClientPlayerJoin(
                net.minecraft.client.Minecraft.getInstance().player);
          }
        });
  }
}
