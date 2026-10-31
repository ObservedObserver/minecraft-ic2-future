package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.Ic2ItemGroupType;
import ic2.core.block.machine.tileentity.TileEntityExtractor;
import ic2.core.block.machine.tileentity.TileEntityMacerator;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.item.ElectricItemManager;
import ic2.core.ref.Ic2Blocks;
import ic2.core.ref.Ic2Items;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;

/** Loaded recipes, powered processing, partial batches, and the complete sulfur recycling loop. */
public final class RecipeExpansionGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void saltpeterIsRegisteredTaggedAndAvailableInMaterials(GameTestHelper h) {
    Item saltpeter = saltpeter(h);
    h.assertTrue(new ItemStack(saltpeter).is(TagKey.create(Registries.ITEM,
        Identifier.parse("c:dusts/saltpeter"))), "saltpeter common dust tag loaded");
    h.assertTrue(Ic2Items.CREATIVE_TAB_ITEMS.get(Ic2ItemGroupType.MATERIALS).stream()
        .anyMatch(supplier -> supplier.get() == saltpeter), "saltpeter in materials creative tab");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void threeDustRecipeIsVisibleAndWorksInBothCraftingGrids(GameTestHelper h) {
    List<Item> dusts = List.of(saltpeter(h), Ic2Items.COAL_DUST, Ic2Items.SULFUR_DUST);
    for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
      if (a == b) continue;
      int c = 3 - a - b;
      for (int width : List.of(2, 3)) {
        var stacks = emptyGrid(width * width);
        stacks.set(0, new ItemStack(dusts.get(a)));
        stacks.set(width * width - 2, new ItemStack(dusts.get(b)));
        stacks.set(width * width - 1, new ItemStack(dusts.get(c)));
        var input = CraftingInput.of(width, width, stacks);
        var recipe = h.getLevel().getServer().getRecipeManager()
            .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value() instanceof ic2.core.recipe.AdvShapelessRecipe r && !r.hidden,
            "new gunpowder recipe must be publicly visible");
        assertStack(h, recipe.value().assemble(input), Items.GUNPOWDER, 3, "three dust crafting");
      }
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void missingWrongAndExtraIngredientsCannotCraftGunpowder(GameTestHelper h) {
    Item nitrate = saltpeter(h);
    for (List<Item> invalid : List.of(
        List.of(nitrate, Ic2Items.COAL_DUST),
        List.of(nitrate, Ic2Items.SULFUR_DUST),
        List.of(Ic2Items.COAL_DUST, Ic2Items.SULFUR_DUST),
        List.of(nitrate, Items.COAL, Ic2Items.SULFUR_DUST),
        List.of(nitrate, Ic2Items.COAL_DUST, Items.SULFUR),
        List.of(nitrate, Ic2Items.COAL_DUST, Items.REDSTONE),
        List.of(nitrate, Ic2Items.COAL_DUST, Ic2Items.SULFUR_DUST, Items.REDSTONE))) {
      var stacks = emptyGrid(9);
      for (int i = 0; i < invalid.size(); i++) stacks.set(i, new ItemStack(invalid.get(i)));
      var input = CraftingInput.of(3, 3, stacks);
      var recipe = h.getLevel().getServer().getRecipeManager()
          .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
      h.assertTrue(recipe.isEmpty() || !recipe.get().value().assemble(input).is(Items.GUNPOWDER),
          "invalid ingredients must not produce gunpowder: " + invalid);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void originalRedstoneGunpowderRecipeIsRetained(GameTestHelper h) {
    var stacks = emptyGrid(9);
    for (int i = 0; i < 9; i++) stacks.set(i,
        new ItemStack(i % 2 == 0 ? Items.REDSTONE : Ic2Items.COAL_DUST));
    assertStack(h, craft(h, CraftingInput.of(3, 3, stacks)), Items.GUNPOWDER, 3,
        "legacy gunpowder recipe");
    h.succeed();
  }

  @GameTest(template = EMPTY, timeoutTicks = 750)
  public static void twoSandstoneExtractExactlyEighteenSaltpeter(GameTestHelper h) {
    var machine = extractor(h);
    machine.inputSlot.put(0, new ItemStack(Items.SANDSTONE, 2));
    h.succeedWhen(() -> {
      assertStack(h, machine.outputSlot.get(0), saltpeter(h), 18, "sandstone extraction");
      h.assertTrue(machine.inputSlot.get(0).isEmpty(), "both sandstone consumed");
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void sandstoneMacerationStillProducesSand(GameTestHelper h) {
    var machine = macerator(h);
    machine.inputSlot.put(0, new ItemStack(Items.SANDSTONE));
    h.succeedWhen(() -> {
      assertStack(h, machine.outputSlot.get(0), Items.SAND, 1, "original sandstone grinding");
      h.assertTrue(machine.inputSlot.get(0).isEmpty(), "sandstone consumed");
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 750)
  public static void fullOutputBlocksExtractionWithoutLosingSandstone(GameTestHelper h) {
    var machine = extractor(h);
    machine.inputSlot.put(0, new ItemStack(Items.SANDSTONE));
    machine.outputSlot.put(0, new ItemStack(saltpeter(h), 64));
    h.runAtTickTime(350, () -> {
      assertStack(h, machine.inputSlot.get(0), Items.SANDSTONE, 1, "blocked input retained");
      assertStack(h, machine.outputSlot.get(0), saltpeter(h), 64, "blocked output unchanged");
      machine.outputSlot.put(0, ItemStack.EMPTY);
    });
    h.succeedWhen(() -> {
      h.assertTrue(h.getTick() > 350, "wait for output slot to be cleared");
      assertStack(h, machine.outputSlot.get(0), saltpeter(h), 9, "resumed sandstone extraction");
      h.assertTrue(machine.inputSlot.get(0).isEmpty(), "input consumed exactly once");
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void unpoweredExtractorDoesNotProduceSaltpeter(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.EXTRACTOR);
    var machine = h.getBlockEntity(POS, TileEntityExtractor.class);
    machine.inputSlot.put(0, new ItemStack(Items.SANDSTONE));
    h.runAtTickTime(350, () -> {
      h.assertTrue(machine.outputSlot.get(0).isEmpty(), "no output without power");
      assertStack(h, machine.inputSlot.get(0), Items.SANDSTONE, 1, "unpowered input retained");
      h.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 750)
  public static void sevenGunpowderYieldTwoSulfurAndLeaveOne(GameTestHelper h) {
    var machine = extractor(h);
    machine.inputSlot.put(0, new ItemStack(Items.GUNPOWDER, 7));
    h.runAtTickTime(680, () -> {
      assertStack(h, machine.outputSlot.get(0), Ic2Items.SULFUR_DUST, 2, "two complete batches");
      assertStack(h, machine.inputSlot.get(0), Items.GUNPOWDER, 1, "partial batch retained");
      h.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void twoGunpowderCannotStartSulfurExtraction(GameTestHelper h) {
    var machine = extractor(h);
    machine.inputSlot.put(0, new ItemStack(Items.GUNPOWDER, 2));
    h.runAtTickTime(350, () -> {
      h.assertTrue(machine.outputSlot.get(0).isEmpty(), "incomplete gunpowder batch produces nothing");
      assertStack(h, machine.inputSlot.get(0), Items.GUNPOWDER, 2, "partial batch not consumed");
      h.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 800)
  public static void charcoalToGunpowderToSulfurCannotMultiplySulfur(GameTestHelper h) {
    var grinder = macerator(h);
    grinder.inputSlot.put(0, new ItemStack(Items.CHARCOAL));
    h.runAtTickTime(350, () -> {
      assertStack(h, grinder.outputSlot.get(0), Ic2Items.COAL_DUST, 1, "charcoal grinding");
      var input = CraftingInput.of(2, 2, List.of(new ItemStack(saltpeter(h)),
          grinder.outputSlot.get(0).copy(), new ItemStack(Ic2Items.SULFUR_DUST), ItemStack.EMPTY));
      ItemStack gunpowder = craft(h, input);
      assertStack(h, gunpowder, Items.GUNPOWDER, 3, "charcoal gunpowder crafting");
      h.setBlock(POS, Blocks.AIR);
      var machine = extractor(h);
      machine.inputSlot.put(0, gunpowder);
    });
    h.succeedWhen(() -> {
      h.assertTrue(h.getTick() > 350, "wait for charcoal processing and crafting");
      var machine = h.getBlockEntity(POS, TileEntityExtractor.class);
      assertStack(h, machine.outputSlot.get(0), Ic2Items.SULFUR_DUST, 1,
          "complete loop returns exactly the one sulfur invested");
      h.assertTrue(machine.inputSlot.get(0).isEmpty(), "all three gunpowder consumed");
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void spiderEyeStillYieldsTwoPowderAndCraftsWeedEx(GameTestHelper h) {
    grindAndCraftWeedEx(h, Items.SPIDER_EYE, 2);
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void poisonousPotatoYieldsTwoPowderAndCraftsWeedEx(GameTestHelper h) {
    grindAndCraftWeedEx(h, Items.POISONOUS_POTATO, 2);
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void pufferfishYieldsTwoPowderAndCraftsWeedEx(GameTestHelper h) {
    grindAndCraftWeedEx(h, Items.PUFFERFISH, 2);
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void fermentedSpiderEyeYieldsFourPowderAndCraftsWeedEx(GameTestHelper h) {
    grindAndCraftWeedEx(h, Items.FERMENTED_SPIDER_EYE, 4);
  }

  @GameTest(template = EMPTY, timeoutTicks = 750)
  public static void nineRottenFleshYieldTwoPowderLeaveOneAndCraftWeedEx(GameTestHelper h) {
    var machine = macerator(h);
    machine.inputSlot.put(0, new ItemStack(Items.ROTTEN_FLESH, 9));
    h.runAtTickTime(680, () -> {
      assertStack(h, machine.outputSlot.get(0), Ic2Items.GRIN_POWDER, 2, "rotten flesh full batches");
      assertStack(h, machine.inputSlot.get(0), Items.ROTTEN_FLESH, 1, "rotten flesh remainder");
      assertWeedExCrafting(h, machine.outputSlot.get(0));
      h.succeed();
    });
  }

  @GameTest(template = EMPTY, timeoutTicks = 400)
  public static void threeRottenFleshCannotStartPowderProduction(GameTestHelper h) {
    var machine = macerator(h);
    machine.inputSlot.put(0, new ItemStack(Items.ROTTEN_FLESH, 3));
    h.runAtTickTime(350, () -> {
      h.assertTrue(machine.outputSlot.get(0).isEmpty(), "incomplete rotten flesh batch produces nothing");
      assertStack(h, machine.inputSlot.get(0), Items.ROTTEN_FLESH, 3, "rotten flesh retained");
      h.succeed();
    });
  }

  private static void grindAndCraftWeedEx(GameTestHelper h, Item ingredient, int count) {
    var machine = macerator(h);
    machine.inputSlot.put(0, new ItemStack(ingredient));
    h.succeedWhen(() -> {
      assertStack(h, machine.outputSlot.get(0), Ic2Items.GRIN_POWDER, count, "powder grinding");
      h.assertTrue(machine.inputSlot.get(0).isEmpty(), "ingredient consumed");
      assertWeedExCrafting(h, machine.outputSlot.get(0));
    });
  }

  private static void assertWeedExCrafting(GameTestHelper h, ItemStack powder) {
    for (boolean bucket : List.of(false, true)) {
      var input = CraftingInput.of(2, 2, List.of(new ItemStack(bucket ? Items.BUCKET : Ic2Items.EMPTY_CELL),
          new ItemStack(Items.REDSTONE), powder.copyWithCount(1), ItemStack.EMPTY));
      assertStack(h, craft(h, input), bucket
          ? BuiltInRegistries.ITEM.getValue(Identifier.parse("ic2:weed_ex_bucket"))
          : Ic2Items.WEED_EX_CELL, 1,
          "powder must work in existing weed-ex bucket and cell recipes");
    }
  }

  private static Item saltpeter(GameTestHelper h) {
    var id = Identifier.parse("ic2:saltpeter_dust");
    Item item = BuiltInRegistries.ITEM.getValue(id);
    h.assertTrue(item != null && BuiltInRegistries.ITEM.getKey(item).equals(id),
        "saltpeter item must be registered");
    return item;
  }

  private static ArrayList<ItemStack> emptyGrid(int size) {
    var items = new ArrayList<ItemStack>();
    for (int i = 0; i < size; i++) items.add(ItemStack.EMPTY);
    return items;
  }

  private static ItemStack craft(GameTestHelper h, CraftingInput input) {
    return h.getLevel().getServer().getRecipeManager()
        .getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
        .orElseThrow(() -> new AssertionError("expected loaded crafting recipe")).value().assemble(input);
  }

  private static TileEntityMacerator macerator(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.MACERATOR);
    var machine = h.getBlockEntity(POS, TileEntityMacerator.class);
    power(machine);
    return machine;
  }

  private static TileEntityExtractor extractor(GameTestHelper h) {
    h.setBlock(POS, Ic2Blocks.EXTRACTOR);
    var machine = h.getBlockEntity(POS, TileEntityExtractor.class);
    power(machine);
    return machine;
  }

  private static void power(TileEntityStandardMachine<?, ?, ?> machine) {
    machine.dischargeSlot.put(0, ElectricItemManager.getCharged(Ic2Items.RE_BATTERY, Double.POSITIVE_INFINITY));
  }

  private static void assertStack(GameTestHelper h, ItemStack stack, Item expected, int count, String message) {
    h.assertTrue(stack.is(expected), message + ": item, got " + stack);
    h.assertValueEqual(stack.getCount(), count, message + ": count");
  }
}
