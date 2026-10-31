package diagnostics;
import java.nio.file.Path;
import net.minecraft.nbt.*;
public final class InspectPlayerNbt {
  public static void main(String[] args) throws Exception {
    CompoundTag root = NbtIo.readCompressed(Path.of(args[0]), NbtAccounter.unlimitedHeap());
    CompoundTag tag = root;
    if (root.contains("Data")) tag = root.getCompoundOrEmpty("Data");
    System.out.println("ROOT_KEYS=" + tag.keySet());
    if (tag.contains("Player")) tag = tag.getCompoundOrEmpty("Player");
    System.out.println("PLAYER_POSITION=" + tag.get("Pos"));
    System.out.println("PLAYER_INVENTORY=" + tag.get("Inventory"));
    if (args.length > 1 && args[1].equals("seed-test-items")) {
      ListTag inventory = new ListTag();
      String[] ids = {"ic2:re_battery", "ic2:empty_cell", "ic2:water_cell", "ic2:macerator", "ic2:insulated_copper_cable"};
      for (int i=0;i<ids.length;i++) {
        CompoundTag item = new CompoundTag(); item.putByte("Slot", (byte)i);
        item.putString("id",ids[i]); item.putInt("count",i==1?2:1); inventory.add(item);
      }
      tag.put("Inventory",inventory); tag.putInt("SelectedItemSlot",0);
      NbtIo.writeCompressed(root, Path.of(args[0]));
      System.out.println("TEST_FIXTURE_ITEMS_WRITTEN="+inventory);
    }
  }
}
