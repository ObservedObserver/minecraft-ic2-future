package ic2.forge;

import ic2.compat.client.LegacyRenderHooks.BlockColor;
import ic2.compat.client.LegacyRenderHooks.ItemColor;
import ic2.compat.client.LegacyRenderHooks.Property;
import ic2.core.fluid.Ic2FluidStack;
import ic2.core.proxy.ClientEnvProxy;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.loading.FMLPaths;

public final class ClientEnvProxyForge implements ClientEnvProxy {
  static List<ClientEnvProxyForge.ScreenRegistration<?>> screenRegistrations = new ArrayList<>();
  static List<ClientEnvProxyForge.BlockColorProviderRegistration> blockColorProviderRegistrations =
      new ArrayList<>();
  static List<ClientEnvProxyForge.ItemColorProviderRegistration> itemColorProviderRegistrations =
      new ArrayList<>();
  static List<KeyMapping> keyBindingRegistrations = new ArrayList<>();
  static List<ClientEnvProxyForge.BerRegistration<?, ?>> berRegistrations = new ArrayList<>();
  static List<ClientEnvProxyForge.EntityRendererRegistration<?>> entityRendererRegistrations =
      new ArrayList<>();
  static List<ClientEnvProxyForge.BlockEntityRendererRegistration<?, ?>>
      blockEntityRendererRegistrations = new ArrayList<>();

  @Override
  public <H extends AbstractContainerMenu> void registerScreen(
      MenuType<H> type, ClientEnvProxy.ScreenFactory<H> factory) {
    screenRegistrations.add(new ClientEnvProxyForge.ScreenRegistration<>(type, factory));
  }

  @Override
  public File getMinecraftDir() {
    return FMLPaths.GAMEDIR.get().toFile();
  }

  @Override
  public void registerColorProvider(BlockColor provider, Block... blocks) {
    blockColorProviderRegistrations.add(
        new ClientEnvProxyForge.BlockColorProviderRegistration(provider, blocks));
  }

  @Override
  public void registerColorProvider(ItemColor provider, ItemLike... items) {
    for (ItemLike item : items)
      ic2.compat.client.LegacyRenderHooks.ITEM_COLORS.put(item.asItem(), provider);
    itemColorProviderRegistrations.add(
        new ClientEnvProxyForge.ItemColorProviderRegistration(provider, items));
  }

  @Override
  public void registerKeyBinding(KeyMapping keyBinding) {
    keyBindingRegistrations.add(keyBinding);
  }

  @Override
  public <
          E extends BlockEntity,
          S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      void registerBer(BlockEntityType<E> type, BlockEntityRendererProvider<E, S> rendererFactory) {
    berRegistrations.add(new ClientEnvProxyForge.BerRegistration<>(type, rendererFactory));
  }

  @Override
  public void registerModelPredicateProvider(Identifier id, Property predicateProvider) {
    ic2.compat.client.LegacyRenderHooks.GENERIC_PROPERTIES.put(id, predicateProvider);
  }

  @Override
  public void registerModelPredicateProvider(Item item, Identifier id, Property predicateProvider) {
    ic2.compat.client.LegacyRenderHooks.ITEM_PROPERTIES
        .computeIfAbsent(item, key -> new java.util.concurrent.ConcurrentHashMap<>())
        .put(id, predicateProvider);
  }

  @Override
  public <E extends Entity> void registerEntityRenderer(
      EntityType<? extends E> type, EntityRendererProvider<E> factory) {
    entityRendererRegistrations.add(
        new ClientEnvProxyForge.EntityRendererRegistration<>(type, factory));
  }

  @Override
  public <
          E extends BlockEntity,
          S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      void registerBlockEntityRenderer(
          BlockEntityType<? extends E> type, BlockEntityRendererProvider<E, S> factory) {
    blockEntityRendererRegistrations.add(
        new ClientEnvProxyForge.BlockEntityRendererRegistration<>(type, factory));
  }

  @Override
  public TextureAtlasSprite getFluidStillSprite(Ic2FluidStack stack) {
    return net.minecraft.client.Minecraft.getInstance()
        .getModelManager()
        .getFluidStateModelSet()
        .get(stack.getFluid().defaultFluidState())
        .stillMaterial()
        .sprite();
  }

  @Override
  public int getFluidColor(Ic2FluidStack stack) {
    var model =
        net.minecraft.client.Minecraft.getInstance()
            .getModelManager()
            .getFluidStateModelSet()
            .get(stack.getFluid().defaultFluidState());
    return model.fluidTintSource() == null
        ? -1
        : model.fluidTintSource().colorAsStack(EnvFluidHandlerForge.getForgeFs(stack));
  }

  @Override
  public String getFluidName(Ic2FluidStack stack) {
    return Component.translatable(
            stack
                .getFluid()
                .getFluidType()
                .getDescriptionId(EnvFluidHandlerForge.getForgeFs(stack)))
        .getString();
  }

  record ScreenRegistration<H extends AbstractContainerMenu>(
      MenuType<H> type, ClientEnvProxy.ScreenFactory<H> factory) {}

  record BerRegistration<
      T extends BlockEntity,
      S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>(
      BlockEntityType<? extends T> blockEntityType,
      BlockEntityRendererProvider<T, S> blockEntityRendererProvider) {}

  record BlockColorProviderRegistration(BlockColor provider, Block... blocks) {}

  record BlockEntityRendererRegistration<
      B extends BlockEntity,
      S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>(
      BlockEntityType<? extends B> type, BlockEntityRendererProvider<B, S> factory) {}

  record EntityRendererRegistration<E extends Entity>(
      EntityType<? extends E> type, EntityRendererProvider<E> factory) {}

  record ItemColorProviderRegistration(ItemColor provider, ItemLike... items) {}
}
