package ic2.core.event;

import ic2.api.item.ElectricItem;
import ic2.core.GuiOverlayer;
import ic2.core.IC2;
import ic2.core.fluid.FluidHandler;
import ic2.core.item.IHandHeldInventory;
import ic2.core.item.armor.jetpack.JetpackLogic;
import ic2.core.item.tool.AbstractItemNanoSaber;
import ic2.core.item.tool.ContainerToolbox;
import ic2.core.item.upgrade.ItemUpgradeModule;
import ic2.core.network.RpcHandler;
import ic2.core.proxy.SideProxyClient;
import ic2.core.sound.SoundManagerClient;
import ic2.core.util.StackUtil;
import ic2.core.util.Util;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class EventHandlerClient {
  public static void onClientSetup() {
    SideProxyClient.envProxy.registerModelPredicateProvider(
        IC2.getIdentifier("charge"),
        (stack, world, entity, seed) -> (float) ElectricItem.manager.getChargeLevel(stack));

    for (Direction direction : Util.ALL_DIRS) {
      SideProxyClient.envProxy.registerModelPredicateProvider(
          IC2.getIdentifier(direction.getName()),
          (stack, world, entity, seed) ->
              stack.getItem() instanceof ItemUpgradeModule
                      && ItemUpgradeModule.getDirection(stack) == direction
                  ? 1.0F
                  : 0.0F);
    }

    SideProxyClient.envProxy.registerModelPredicateProvider(
        IC2.getIdentifier("is_activated"),
        (stack, world, entity, seed) ->
            stack.getItem() instanceof AbstractItemNanoSaber nanoSaber
                ? nanoSaber.getActiveData(stack, world)
                : 0f);
    SideProxyClient.envProxy.registerModelPredicateProvider(
        IC2.getIdentifier("tool_box_open"),
        (stack, world, entity, seed) -> {
          if (!(entity instanceof Player player)) {
            return 0.0F;
          } else {
            ItemStack mainHandItem;
            boolean open =
                player.containerMenu instanceof ContainerToolbox
                    && (StackUtil.checkItemEquality(mainHandItem = player.getMainHandItem(), stack)
                        || StackUtil.checkItemEquality(player.getOffhandItem(), stack)
                            && !(mainHandItem.getItem() instanceof IHandHeldInventory));
            return open ? 1.0F : 0.0F;
          }
        });
  }

  public static void onSoundSetup() {}

  public static float onSetupFogDensity(BlockState state) {
    Fluid fluid = FluidHandler.getWorldFluid(state);
    if (fluid != null
        && "ic2"
            .equals(Objects.requireNonNull(BuiltInRegistries.FLUID.getKey(fluid)).getNamespace())) {
      int density = FluidHandler.getDensity(fluid);
      return (float) Util.map(Math.abs(density), 20000.0, 2.0);
    } else {
      return -1.0F;
    }
  }

  public static int onRenderFogColor(BlockState state) {
    Fluid fluid = FluidHandler.getWorldFluid(state);
    return fluid != null && "ic2".equals(BuiltInRegistries.FLUID.getKey(fluid).getNamespace())
        ? FluidHandler.getColor(fluid)
        : -1;
  }

  public static void onGuiCreate(
      Screen screen, List<GuiEventListener> widgets, Consumer<GuiEventListener> widgetAdder) {}

  private static final GuiOverlayer guiOverlayer =
      new GuiOverlayer(net.minecraft.client.Minecraft.getInstance());

  public static void onRenderHotBar(GuiGraphicsExtractor guiGraphics) {
    guiOverlayer.render(guiGraphics);
  }

  public static SoundInstance onSoundPlayed(SoundInstance sound) {
    return SoundManagerClient.onSoundPlayed(sound);
  }

  public static void onDisconnect() {
    RpcHandler.onDisconnect();
    // Ensure looping jetpack audio cannot survive into the next multiplayer session.
    JetpackLogic.stopJetpackSound(null);
  }

  public static void onClientPlayerJoin(Player player) {
    if (!player.level().isClientSide()) {
      return;
    }

    // Reset client-global jetpack audio when entering a new client level/session.
    JetpackLogic.stopJetpackSound(null);
  }
}
