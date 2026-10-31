package ic2.core.proxy;

import ic2.compat.client.LegacyRenderHooks.BlockColor;
import ic2.compat.client.LegacyRenderHooks.ItemColor;
import ic2.compat.client.LegacyRenderHooks.Property;
import ic2.core.fluid.Ic2FluidStack;
import java.io.File;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface ClientEnvProxy {
  <H extends AbstractContainerMenu> void registerScreen(
      MenuType<H> var1, ClientEnvProxy.ScreenFactory<H> var2);

  File getMinecraftDir();

  void registerColorProvider(BlockColor var1, Block... var2);

  void registerColorProvider(ItemColor var1, ItemLike... var2);

  void registerKeyBinding(KeyMapping var1);

  <
          E extends BlockEntity,
          S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      void registerBer(BlockEntityType<E> var1, BlockEntityRendererProvider<E, S> var2);

  void registerModelPredicateProvider(Identifier var1, Property var2);

  void registerModelPredicateProvider(Item var1, Identifier var2, Property var3);

  <E extends Entity> void registerEntityRenderer(
      EntityType<? extends E> var1, EntityRendererProvider<E> var2);

  <
          E extends BlockEntity,
          S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
      void registerBlockEntityRenderer(
          BlockEntityType<? extends E> var1, BlockEntityRendererProvider<E, S> var2);

  TextureAtlasSprite getFluidStillSprite(Ic2FluidStack var1);

  int getFluidColor(Ic2FluidStack var1);

  String getFluidName(Ic2FluidStack var1);

  default ClientEnvProxy.QuadData getQuadData(BakedQuad quad) {
    final float[] positions = new float[12];
    final float[] uvs = new float[8];
    for (int i = 0; i < 4; i++) {
      var position = quad.position(i);
      positions[i * 3] = position.x();
      positions[i * 3 + 1] = position.y();
      positions[i * 3 + 2] = position.z();
      uvs[i * 2] = net.minecraft.client.model.geom.builders.UVPair.unpackU(quad.packedUV(i));
      uvs[i * 2 + 1] = net.minecraft.client.model.geom.builders.UVPair.unpackV(quad.packedUV(i));
    }

    return new ClientEnvProxy.QuadData() {
      @Override
      public float[] positions() {
        return positions;
      }

      @Override
      public float[] uvs() {
        return uvs;
      }

      @Override
      public int tint() {
        return quad.materialInfo().tintIndex();
      }

      @Override
      public TextureAtlasSprite sprite() {
        return quad.materialInfo().sprite();
      }
    };
  }

  interface QuadData {
    float[] positions();

    float[] uvs();

    int tint();

    TextureAtlasSprite sprite();
  }

  interface ScreenFactory<H extends AbstractContainerMenu> {
    AbstractContainerScreen<H> create(H var1, Inventory var2, Component var3);
  }
}
