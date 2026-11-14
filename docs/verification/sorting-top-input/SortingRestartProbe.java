package sortingprobe;

import ic2.core.block.machine.tileentity.TileEntitySortingMachine;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Temporary, isolated validation hook; never included in the delivered JAR. */
public final class SortingRestartProbe {
  private static int ticks;
  private static final BlockPos MACHINE = new BlockPos(8191, 80, 8192);
  private static final BlockPos DESTINATION = MACHINE.east();
  private static final BlockPos SUPPLY = MACHINE.above().east();

  public static void tick(ServerTickEvent.Post event) {
    String phase = System.getenv("IC2_SORTING_RESTART_PHASE");
    if (phase == null || ++ticks != 10) return;
    MinecraftServer server = event.getServer();
    ServerLevel level = server.overworld();
    Path report = Path.of(System.getenv("IC2_SORTING_RESTART_REPORT"));
    try {
      if (phase.equals("setup")) {
        var left = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
            .setValue(ChestBlock.TYPE, ChestType.LEFT);
        var right = left.setValue(ChestBlock.TYPE, ChestType.RIGHT);
        level.setBlockAndUpdate(MACHINE, ic2.core.ref.Ic2Blocks.SORTING_MACHINE.defaultBlockState());
        level.setBlockAndUpdate(DESTINATION, Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(MACHINE.above(), left);
        level.setBlockAndUpdate(SUPPLY, right);
        var te = (TileEntitySortingMachine) level.getBlockEntity(MACHINE);
        te.getComponent(ic2.core.block.comp.Energy.class).addEnergy(1000);
        var named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("restart sample"));
        te.getFilterSlots(Direction.EAST)[0] = named.copyWithCount(4);
        te.setChanged();
        ((ChestBlockEntity) level.getBlockEntity(SUPPLY)).setItem(0, named.copyWithCount(5));
        for (int i = 0; i < 3; i++) tickMachine(te);
        check(level, 2, 3, 0, 1000);
      } else if (phase.equals("verify")) {
        check(level, 2, 3, 0, 1000);
        var te = (TileEntitySortingMachine) level.getBlockEntity(MACHINE);
        tickMachine(te);
        check(level, 1, 0, 4, 920);
      } else if (phase.equals("verify-final")) {
        check(level, 1, 0, 4, 920);
      } else throw new IllegalArgumentException(phase);
      if (!server.saveEverything(false, true, true)) throw new AssertionError("saveEverything failed");
      Files.writeString(report, "{\"phase\":\"" + phase + "\",\"passed\":true}\n");
    } catch (Throwable error) {
      error.printStackTrace();
      try { Files.writeString(report, "{\"phase\":\"" + phase + "\",\"passed\":false}\n"); }
      catch (Exception ignored) { }
    } finally { server.halt(false); }
  }

  private static void tickMachine(TileEntitySortingMachine te) throws Exception {
    var method = TileEntitySortingMachine.class.getDeclaredMethod("updateEntityServer");
    method.setAccessible(true);
    method.invoke(te);
  }

  private static void check(ServerLevel level, int sourceCount, int bufferCount, int destinationCount,
      int energy) {
    var te = (TileEntitySortingMachine) level.getBlockEntity(MACHINE);
    var source = (ChestBlockEntity) level.getBlockEntity(SUPPLY);
    var destination = (ChestBlockEntity) level.getBlockEntity(DESTINATION);
    if (te == null || source == null || destination == null) throw new AssertionError("missing fixture");
    String location = TileEntitySortingMachine.class.getProtectionDomain().getCodeSource().getLocation().toString();
    if (!location.contains("local.21-topinput.1.jar")) throw new AssertionError("Wrong production classes: " + location);
    System.out.println("SORTING_PRODUCTION_JAR " + location);
    int buffered = 0;
    for (ItemStack stack : te.buffer) if (stack.is(Items.IRON_INGOT)) buffered += stack.getCount();
    int supplied = 0;
    int delivered = 0;
    for (int i = 0; i < source.getContainerSize(); i++) supplied += source.getItem(i).getCount();
    for (int i = 0; i < destination.getContainerSize(); i++) delivered += destination.getItem(i).getCount();
    ItemStack rule = te.getFilterSlots(Direction.EAST)[0];
    if (supplied != sourceCount || buffered != bufferCount || delivered != destinationCount
        || (int) te.getEnergy() != energy || rule.getCount() != 4
        || !Component.literal("restart sample").equals(rule.get(DataComponents.CUSTOM_NAME))
        || te.defaultRoute != Direction.DOWN) {
      throw new AssertionError("source=" + supplied + ", buffer=" + buffered + ", east=" + delivered
          + ", energy=" + te.getEnergy() + ", rule=" + rule);
    }
    for (ItemStack stack : te.buffer) if (!stack.isEmpty()
        && !Component.literal("restart sample").equals(stack.get(DataComponents.CUSTOM_NAME)))
      throw new AssertionError("buffer component lost");
    if (destinationCount > 0 && !Component.literal("restart sample").equals(
        destination.getItem(0).get(DataComponents.CUSTOM_NAME)))
      throw new AssertionError("output component lost");
    System.out.println("SORTING_RESTART_PROBE source=" + supplied + " buffer=" + buffered
        + " east=" + delivered + " energy=" + te.getEnergy());
  }
}
