package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.item.ElectricItemManager;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** Exercises the loaded recipes and powered machines, including insufficient batch inputs. */
public class SulfurIntegrationGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void nineDustCraftsOneBlockAndGrindsBackWithoutGain(GameTestHelper helper) {
    var items = new ArrayList<ItemStack>();
    for (int i = 0; i < 9; i++) items.add(new ItemStack(Ic2Items.SULFUR_DUST));
    var input = CraftingInput.of(3, 3, items);
    var manager = helper.getLevel().getServer().getRecipeManager();
    var recipe =
        manager
            .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
            .orElseThrow(() -> new AssertionError("nine sulfur dust must have a loaded recipe"));
    ItemStack block = recipe.value().assemble(input);
    helper.assertTrue(block.is(Items.SULFUR), "crafting must produce vanilla sulfur");
    helper.assertValueEqual(block.getCount(), 1, "sulfur block count");

    var incomplete = new ArrayList<>(items);
    incomplete.set(4, ItemStack.EMPTY);
    helper.assertTrue(
        !recipe.value().matches(CraftingInput.of(3, 3, incomplete), helper.getLevel()),
        "eight dust must not craft a block");
    helper.assertTrue(
        !recipe
            .value()
            .matches(
                CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(Items.SULFUR))),
                helper.getLevel()),
        "sulfur blocks must not substitute for sulfur dust");

    TileEntityMacerator machine = poweredMacerator(helper);
    machine.inputSlot.put(0, block);
    helper.succeedWhen(
        () -> {
          assertDust(helper, machine, 9);
          helper.assertTrue(machine.inputSlot.get(0).isEmpty(), "block must be consumed");
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 750)
  public static void eightSpikesProduceExactlyEighteenDust(GameTestHelper helper) {
    TileEntityMacerator machine = poweredMacerator(helper);
    machine.inputSlot.put(0, new ItemStack(Items.SULFUR_SPIKE, 8));
    helper.succeedWhen(
        () -> {
          assertDust(helper, machine, 18);
          helper.assertTrue(
              machine.inputSlot.get(0).isEmpty(), "all eight spikes must be consumed");
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void fiveSpikesConsumeFourAndLeaveOne(GameTestHelper helper) {
    TileEntityMacerator machine = poweredMacerator(helper);
    machine.inputSlot.put(0, new ItemStack(Items.SULFUR_SPIKE, 5));
    helper.runAtTickTime(
        350,
        () -> {
          assertDust(helper, machine, 9);
          helper.assertTrue(machine.inputSlot.get(0).is(Items.SULFUR_SPIKE), "leftover spike item");
          helper.assertValueEqual(machine.inputSlot.get(0).getCount(), 1, "leftover spike count");
          helper.succeed();
        });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void threeSpikesCannotStartProcessing(GameTestHelper helper) {
    TileEntityMacerator machine = poweredMacerator(helper);
    machine.inputSlot.put(0, new ItemStack(Items.SULFUR_SPIKE, 3));
    helper.runAtTickTime(
        350,
        () -> {
          helper.assertTrue(
              machine.outputSlot.get(0).isEmpty(), "partial batch must not yield dust");
          helper.assertValueEqual(machine.inputSlot.get(0).getCount(), 3, "partial batch retained");
          helper.succeed();
        });
  }

  private static TileEntityMacerator poweredMacerator(GameTestHelper helper) {
    helper.setBlock(MACHINE_POS, Ic2Blocks.MACERATOR);
    var machine = helper.getBlockEntity(MACHINE_POS, TileEntityMacerator.class);
    machine.dischargeSlot.put(
        0, ElectricItemManager.getCharged(Ic2Items.RE_BATTERY, Double.POSITIVE_INFINITY));
    return machine;
  }

  private static void assertDust(GameTestHelper helper, TileEntityMacerator machine, int count) {
    ItemStack output = machine.outputSlot.get(0);
    helper.assertTrue(output.is(Ic2Items.SULFUR_DUST), "macerator must produce IC2 sulfur dust");
    helper.assertValueEqual(output.getCount(), count, "sulfur dust output count");
  }
}
