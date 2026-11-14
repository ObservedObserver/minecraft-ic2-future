package ic2.core.block.machine.container;

import ic2.core.block.machine.tileentity.TileEntitySortingMachine;
import ic2.core.ref.Ic2ScreenHandlers;
import ic2.core.slot.SlotHologramSlot;
import ic2.core.slot.SlotInvSlot;
import ic2.core.util.Util;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ContainerSortingMachine extends ContainerElectricMachine<TileEntitySortingMachine> {
  public ContainerSortingMachine(
      int syncId, Inventory playerInventory, TileEntitySortingMachine be) {
    super(Ic2ScreenHandlers.SORTING_MACHINE, syncId, playerInventory, be, 243, 188, 219);

    for (int i = 0; i < 3; i++) {
      this.addSlot(new SlotInvSlot(be.upgradeSlot, i, 188, 161 + i * 18));
    }

    for (int i = 0; i < 11; i++) {
      this.addSlot(new SlotInvSlot(be.buffer, i, 8 + i * 18, 141));
    }

    for (int i = 0; i < Util.ALL_DIRS.length; i++) {
      Direction dir = Util.ALL_DIRS[i];
      ItemStack[] filterSlots = be.getFilterSlots(dir);
      if (dir == Direction.UP) {
        // Keep the old menu indices so both versions agree on the other five rows.
        SimpleContainer placeholders = new SimpleContainer(filterSlots.length);
        for (int j = 0; j < filterSlots.length; j++) {
          this.addSlot(new Slot(placeholders, j, 80 + j * 18, 19 + i * 20) {
            @Override
            public boolean isActive() {
              return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
              return false;
            }

            @Override
            public boolean mayPickup(Player player) {
              return false;
            }

            @Override
            public void set(ItemStack stack) {
              // Ignore old-server ghost stacks. The persisted top filters live on the machine.
            }
          });
        }
        continue;
      }

      for (int j = 0; j < filterSlots.length; j++) {
        this.addSlot(
            new SlotHologramSlot(
                filterSlots, j, 80 + j * 18, 19 + i * 20, 64, index -> be.setChanged()));
      }
    }
  }

  @Override
  public List<String> getNetworkedFields() {
    List<String> ret = super.getNetworkedFields();
    ret.add("defaultRoute");
    return ret;
  }
}
