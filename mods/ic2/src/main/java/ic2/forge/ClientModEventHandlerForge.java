package ic2.forge;

import ic2.core.event.EventHandlerClient;
import ic2.core.item.armor.jetpack.LayerJetpackOverride;
import ic2.forge.model.BeModelLoader;
import ic2.forge.model.CableModelLoader;
import ic2.forge.model.MaskOverlayItemLoader;
import ic2.forge.model.WallModelLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;

public final class ClientModEventHandlerForge {
  private static <
          T extends BlockEntity,
          S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      void registerBer(
          ClientEnvProxyForge.BerRegistration<T, S> reg,
          EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(reg.blockEntityType(), reg.blockEntityRendererProvider());
  }

  private static <T extends Entity> void registerEntityRenderer(
      ClientEnvProxyForge.EntityRendererRegistration<T> reg,
      EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(reg.type(), reg.factory());
  }

  private static <
          T extends BlockEntity,
          S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      void registerBlockEntityRenderer(
          ClientEnvProxyForge.BlockEntityRendererRegistration<T, S> reg,
          EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(reg.type(), reg.factory());
  }

  private static <H extends AbstractContainerMenu> void registerScreen(
      ClientEnvProxyForge.ScreenRegistration<H> reg, RegisterMenuScreensEvent event) {
    event.register(reg.type(), reg.factory()::create);
  }

  @SubscribeEvent
  public void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
    for (ClientEnvProxyForge.ScreenRegistration<?> reg : ClientEnvProxyForge.screenRegistrations) {
      registerScreen(reg, event);
    }
  }

  @SubscribeEvent
  public void onRegisterBlockColorProviders(RegisterColorHandlersEvent.BlockTintSources event) {
    for (ClientEnvProxyForge.BlockColorProviderRegistration reg :
        ClientEnvProxyForge.blockColorProviderRegistrations) {
      event.register(
          java.util.List.of(ic2.compat.client.LegacyRenderHooks.blockTint(reg.provider(), 0)),
          reg.blocks());
    }
  }

  @SubscribeEvent
  public void onRegisterItemColorProviders(RegisterColorHandlersEvent.ItemTintSources event) {
    event.register(
        ic2.core.IC2.getIdentifier("legacy_tint"), ic2.compat.client.LegacyRenderHooks.Tint.CODEC);
  }

  @SubscribeEvent
  public void onRangeProperties(
      net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent event) {
    event.register(
        ic2.core.IC2.getIdentifier("legacy"), ic2.compat.client.LegacyRenderHooks.Numeric.CODEC);
  }

  @SubscribeEvent
  public void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    for (ClientEnvProxyForge.BerRegistration<?, ?> reg : ClientEnvProxyForge.berRegistrations) {
      registerBer(reg, event);
    }

    for (ClientEnvProxyForge.EntityRendererRegistration<?> reg :
        ClientEnvProxyForge.entityRendererRegistrations) {
      registerEntityRenderer(reg, event);
    }

    for (ClientEnvProxyForge.BlockEntityRendererRegistration<?, ?> reg :
        ClientEnvProxyForge.blockEntityRendererRegistrations) {
      registerBlockEntityRenderer(reg, event);
    }
  }

  @SubscribeEvent
  public void onAddLayers(EntityRenderersEvent.AddLayers event) {
    LayerJetpackOverride.register(event);
  }

  @SubscribeEvent
  public void onBlockStateModels(
      net.neoforged.neoforge.client.event.RegisterBlockStateModels event) {
    event.registerModel(ic2.core.IC2.getIdentifier("be"), BeModelLoader.CODEC);
    event.registerModel(ic2.core.IC2.getIdentifier("cable"), CableModelLoader.CODEC);
    event.registerModel(ic2.core.IC2.getIdentifier("wall"), WallModelLoader.CODEC);
  }

  @SubscribeEvent
  public void onItemModels(net.neoforged.neoforge.client.event.RegisterItemModelsEvent event) {
    event.register(ic2.core.IC2.getIdentifier("mask_overlay"), MaskOverlayItemLoader.CODEC);
  }

  @SubscribeEvent
  public void onGeometryLoaders(ModelEvent.RegisterLoaders event) {
    event.register(ic2.core.IC2.getIdentifier("cable"), new ic2.forge.model.CableGeometryLoader());
  }

  @SubscribeEvent
  public void onRegisterKeybindings(RegisterKeyMappingsEvent event) {
    event.registerCategory(ic2.core.util.KeyboardClient.CATEGORY);
    for (KeyMapping keybinding : ClientEnvProxyForge.keyBindingRegistrations) {
      event.register(keybinding);
    }
  }

  @SubscribeEvent
  public void onSoundSetup(SoundEngineLoadEvent event) {
    EventHandlerClient.onSoundSetup();
  }

  @SubscribeEvent
  public void onFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
    net.minecraft.core.registries.BuiltInRegistries.FLUID.forEach(
        fluid -> {
          var visual = EnvFluidHandlerForge.FLUID_VISUALS.get(fluid.getFluidType());
          if (visual == null) return;
          boolean translucent = (visual.color() >>> 24) < 255;
          event.register(
              new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                  new net.minecraft.client.resources.model.sprite.Material(
                      visual.still(), translucent),
                  new net.minecraft.client.resources.model.sprite.Material(
                      visual.flowing(), translucent),
                  null,
                  net.neoforged.neoforge.client.fluid.FluidTintSources.constant(visual.color())),
              fluid);
        });
  }

  @SubscribeEvent
  public void onFluidExtensions(
      net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
    EnvFluidHandlerForge.FLUID_VISUALS.forEach(
        (type, visual) ->
            event.registerFluidType(
                new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
                  @Override
                  public void modifyFogColor(
                      net.minecraft.client.Camera camera,
                      float partialTick,
                      net.minecraft.client.multiplayer.ClientLevel level,
                      int renderDistance,
                      float darkenWorldAmount,
                      org.joml.Vector4f color) {
                    color.set(
                        ((visual.color() >>> 16) & 255) / 255.0F,
                        ((visual.color() >>> 8) & 255) / 255.0F,
                        (visual.color() & 255) / 255.0F,
                        1.0F);
                  }

                  @Override
                  public void modifyFogRender(
                      net.minecraft.client.Camera camera,
                      net.minecraft.client.renderer.fog.environment.FogEnvironment environment,
                      float renderDistance,
                      float partialTick,
                      net.minecraft.client.renderer.fog.FogData data) {
                    data.environmentalStart = -8.0F;
                    data.environmentalEnd =
                        (float)
                            (ic2.core.util.Util.map(Math.abs(type.getDensity()), 20000.0, 2.0)
                                * 0.5);
                  }
                },
                type));
  }

  @SubscribeEvent
  public void onClientSetup(FMLClientSetupEvent event) {
    EventHandlerClient.onClientSetup();
  }
}
