package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.storage.box.TileEntityWoodenStorageBox;
import ic2.core.ref.Ic2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StorageBoxGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";
  private static final BlockPos BOX_POS = new BlockPos(1, 1, 1);

  @GameTest(template = EMPTY)
  public static void woodenStorageBoxPersistsEnchantedItems(GameTestHelper helper) {
    helper.setBlock(BOX_POS, Ic2Blocks.WOODEN_STORAGE_BOX);
    TileEntityWoodenStorageBox box = getStorageBox(helper);

    HolderLookup.RegistryLookup<Enchantment> enchantments =
        helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    Holder<Enchantment> sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
    ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
    sword.enchant(sharpness, 3);
    box.setItem(0, sword);

    CompoundTag nbt = box.saveWithFullMetadata(helper.getLevel().registryAccess());
    helper.setBlock(BOX_POS, Blocks.AIR);
    helper.setBlock(BOX_POS, Ic2Blocks.WOODEN_STORAGE_BOX);
    TileEntityWoodenStorageBox restored = getStorageBox(helper);
    restored.loadWithComponents(nbt, helper.getLevel().registryAccess());

    ItemStack restoredSword = restored.getItem(0);
    helper.assertTrue(
        restoredSword.is(Items.DIAMOND_SWORD), "restored item should be a diamond sword");
    helper.assertValueEqual(
        EnchantmentHelper.getItemEnchantmentLevel(sharpness, restoredSword),
        3,
        "sharpness level after save and load");
    helper.succeed();
  }

  private static TileEntityWoodenStorageBox getStorageBox(GameTestHelper helper) {
    BlockEntity blockEntity =
        helper.getBlockEntity(BOX_POS, net.minecraft.world.level.block.entity.BlockEntity.class);
    if (!(blockEntity instanceof TileEntityWoodenStorageBox storageBox)) {
      throw new IllegalStateException(
          "expected wooden storage box at " + BOX_POS + ", found " + blockEntity);
    }

    return storageBox;
  }

  private static final BlockPos SOURCE = new BlockPos(2, 1, 2);
  private static final BlockPos DEST = new BlockPos(4, 1, 2);

  private static ic2.core.block.storage.box.TileEntityStorageBox portableBox(
      GameTestHelper h, BlockPos pos) {
    return (ic2.core.block.storage.box.TileEntityStorageBox)
        h.getLevel().getBlockEntity(h.absolutePos(pos));
  }

  private static ItemStack namedSword(GameTestHelper h) {
    ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
    sword.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        .getOrThrow(Enchantments.SHARPNESS), 3);
    sword.setDamageValue(57);
    sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
        net.minecraft.network.chat.Component.literal("Portable sword"));
    return sword;
  }

  private static void placeItem(GameTestHelper h, ItemStack item, BlockPos relative) {
    var player = h.makeMockServerPlayerInLevel();
    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, item);
    BlockPos pos = h.absolutePos(relative);
    player.setPos(pos.getX() + .5, pos.getY() + 3, pos.getZ() + .5);
    var hit = new net.minecraft.world.phys.BlockHitResult(
        net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
    var result = ((net.minecraft.world.item.BlockItem) item.getItem()).place(
        new net.minecraft.world.item.context.BlockPlaceContext(
            player, net.minecraft.world.InteractionHand.MAIN_HAND, item, hit));
    h.assertTrue(result.consumesAction(), "native block placement must succeed");
  }

  private static void moveBox(GameTestHelper h, net.minecraft.world.level.block.Block block) {
    h.setBlock(SOURCE, block);
    var box = portableBox(h, SOURCE);
    ItemStack sword = namedSword(h);
    box.setItem(0, sword.copy());
    box.setItem(box.getContainerSize() - 1, new ItemStack(Items.IRON_INGOT, 23));
    // Three moves exercise actual loot, item serialization and native placement each time.
    BlockPos from = SOURCE, to = DEST;
    for (int round = 0; round < 3; round++) {
      h.assertTrue(h.getLevel().destroyBlock(h.absolutePos(from), true), "box must break");
      var drops = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
          new net.minecraft.world.phys.AABB(h.absolutePos(from)).inflate(1.5));
      h.assertValueEqual(drops.size(), 1, "only the carrier drops, with no loose inventory items");
      ItemStack carrier = drops.getFirst().getItem().copy();
      h.assertTrue(carrier.is(block.asItem()), "drop must be the storage box");
      drops.getFirst().discard(); // Pick up the actual dropped carrier.
      carrier = ic2.compat.NbtCompat.readItem(h.getLevel().registryAccess(),
          ic2.compat.NbtCompat.saveItem(carrier, h.getLevel().registryAccess()));
      placeItem(h, carrier, to);
      var restored = portableBox(h, to);
      h.assertTrue(ItemStack.matches(restored.getItem(0), sword),
          "transport must preserve enchantment, name and durability");
      ItemStack last = restored.getItem(restored.getContainerSize() - 1);
      h.assertTrue(last.is(Items.IRON_INGOT) && last.getCount() == 23,
          "transport must preserve last slot and item count");
      for (int i = 1; i < restored.getContainerSize() - 1; i++) {
        h.assertTrue(restored.getItem(i).isEmpty(), "transport must not duplicate stacks");
      }
      BlockPos previous = from; from = to; to = previous;
    }
    h.succeed();
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void woodenBoxNativeDropThenPlacementKeepsItems(GameTestHelper h) {
    moveBox(h, Ic2Blocks.WOODEN_STORAGE_BOX);
  }
  @GameTest(template = "gametest/empty7x7x7")
  public static void ironBoxNativeDropThenPlacementKeepsItems(GameTestHelper h) {
    moveBox(h, Ic2Blocks.IRON_STORAGE_BOX);
  }
  @GameTest(template = "gametest/empty7x7x7")
  public static void bronzeBoxNativeDropThenPlacementKeepsItems(GameTestHelper h) {
    moveBox(h, Ic2Blocks.BRONZE_STORAGE_BOX);
  }
  @GameTest(template = "gametest/empty7x7x7")
  public static void steelBoxNativeDropThenPlacementKeepsItems(GameTestHelper h) {
    moveBox(h, Ic2Blocks.STEEL_STORAGE_BOX);
  }
  @GameTest(template = "gametest/empty7x7x7")
  public static void iridiumBoxNativeDropThenPlacementKeepsItems(GameTestHelper h) {
    moveBox(h, Ic2Blocks.IRIDIUM_STORAGE_BOX);
  }

  @GameTest(template = "gametest/empty7x7x7")
  public static void wrenchDropKeepsInventoryAndOtherCustomData(GameTestHelper h) {
    h.setBlock(SOURCE, Ic2Blocks.WOODEN_STORAGE_BOX);
    var box = portableBox(h, SOURCE);
    ItemStack sword = namedSword(h); box.setItem(0, sword.copy());
    var block = (ic2.core.block.tileentity.Ic2TileEntityBlock) Ic2Blocks.WOODEN_STORAGE_BOX;
    ItemStack carrier = block.getWrenchDrops(h.getLevel(), h.absolutePos(SOURCE),
        box.getBlockState(), box, h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL), 0)
        .getFirst();
    CompoundTag tag = ic2.core.util.StackUtil.getOrCreateNbtData(carrier);
    tag.putString("unrelated", "keep"); ic2.core.util.StackUtil.setTag(carrier, tag);
    carrier = box.adjustDrop(carrier, true);
    h.assertValueEqual(ic2.core.util.StackUtil.getTag(carrier).getStringOr("unrelated", ""),
        "keep", "other custom data survives inventory update");
    placeItem(h, carrier, DEST);
    h.assertTrue(ItemStack.matches(portableBox(h, DEST).getItem(0), sword),
        "wrench drop must retain full item components");
    h.succeed();
  }

  @GameTest(template = EMPTY)
  public static void emptyBoxDropClearsStaleInventory(GameTestHelper h) {
    h.setBlock(BOX_POS, Ic2Blocks.WOODEN_STORAGE_BOX);
    var box = portableBox(h, BOX_POS); box.setItem(0, new ItemStack(Items.DIAMOND, 7));
    ItemStack carrier = box.adjustDrop(new ItemStack(Ic2Blocks.WOODEN_STORAGE_BOX), false);
    box.clearContent();
    carrier = box.adjustDrop(carrier, false);
    h.assertFalse(carrier.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA),
        "empty box must not carry stale inventory data or unnecessary empty custom data");
    h.succeed();
  }
}
