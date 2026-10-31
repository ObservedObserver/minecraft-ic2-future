package ic2.forge;

import ic2.api.item.INanoSaberState;
import ic2.core.IC2;
import ic2.core.block.comp.Fluids;
import ic2.core.block.tileentity.Ic2TileEntity;
import ic2.core.fluid.Ic2FluidItem;
import ic2.core.item.tool.AbstractItemNanoSaber;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

public final class Ic2Capabilities {
  public static final ItemCapability<INanoSaberState, Void> NANO_SABER_STATE =
      ItemCapability.createVoid(IC2.getIdentifier("nano_saber_state"), INanoSaberState.class);

  private Ic2Capabilities() {}

  public static void register(RegisterCapabilitiesEvent event) {
    for (BlockEntityType<?> type : ic2.core.ref.Ic2BlockEntities.allTypes()) {
      event.registerBlockEntity(
          Capabilities.Fluid.BLOCK, type, (be, side) -> fluidHandler(be, side));
      event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> itemHandler(be, side));
    }

    List<Item> fluidItems = new ArrayList<>();
    List<Item> nanoSabers = new ArrayList<>();
    for (Item item : BuiltInRegistries.ITEM) {
      if (item instanceof Ic2FluidItem) {
        fluidItems.add(item);
      }
      if (item instanceof AbstractItemNanoSaber) {
        nanoSabers.add(item);
      }
    }

    if (!fluidItems.isEmpty()) {
      event.registerItem(
          Capabilities.Fluid.ITEM,
          (stack, ctx) -> new ItemFluidResourceHandler(ctx),
          fluidItems.toArray(new Item[0]));
    }
    if (!nanoSabers.isEmpty()) {
      event.registerItem(
          NANO_SABER_STATE,
          (stack, ctx) -> new NanoSaberStateImpl(stack),
          nanoSabers.toArray(new Item[0]));
    }
  }

  private static ResourceHandler<FluidResource> fluidHandler(BlockEntity be, Direction side) {
    if (!(be instanceof Ic2TileEntity tile)) return null;
    Fluids fluids = tile.getComponent(Fluids.class);
    return fluids != null ? new BlockFluidCapImpl(fluids, be, side) : null;
  }

  private static ResourceHandler<ItemResource> itemHandler(BlockEntity be, Direction side) {
    if (!(be instanceof Ic2TileEntity)) {
      return null;
    }

    if (be instanceof WorldlyContainer wc) {
      return side != null ? new WorldlyContainerWrapper(wc, side) : null;
    } else if (be instanceof Container container) {
      return VanillaContainerWrapper.of(container);
    }

    return null;
  }
}
