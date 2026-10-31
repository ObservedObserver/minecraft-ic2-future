package ic2.core.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ic2.compat.GameTest;
import ic2.core.entity.block.ITntEntity;
import ic2.core.entity.block.NukeEntity;
import ic2.core.ref.Ic2Items;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public class TranslationGameTests {
  private static final String TEMPLATE = "gametest/empty3x3x3";

  // Primed ITNT/Nuke entities had no entity.ic2.* lang keys, so death screens and F3 showed the
  // raw translation key (upstream ebc73b78). NeoForge loads mod en_us on the server, so getName
  // must resolve to a real name here.
  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void primedExplosiveEntitiesHaveTranslatedNames(GameTestHelper helper) {
    Entity itnt = new ITntEntity(helper.getLevel(), 0.0, 0.0, 0.0);
    Entity nuke = new NukeEntity(helper.getLevel(), 0.0, 0.0, 0.0, 1.0F, 0);

    assertTranslated(helper, itnt, "entity.ic2.itnt");
    assertTranslated(helper, nuke, "entity.ic2.nuke");
    helper.succeed();
  }

  // Check the actual registered items and their default name components, not only language JSON.
  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void registeredItemNamesExistInEnglishAndChinese(GameTestHelper helper) {
    JsonObject english = readLanguage("en_us");
    JsonObject chinese = readLanguage("zh_cn");
    int items = 0;
    int blockItems = 0;
    for (var item : BuiltInRegistries.ITEM) {
      var id = BuiltInRegistries.ITEM.getKey(item);
      if (!id.getNamespace().equals("ic2")) continue;
      items++;
      if (item instanceof BlockItem) blockItems++;
      String key = item.getDescriptionId();
      helper.assertTrue(english.has(key), id + " has no English name for " + key);
      helper.assertTrue(chinese.has(key), id + " has no Chinese name for " + key);
      var name = new ItemStack(item).get(DataComponents.ITEM_NAME);
      helper.assertTrue(
          name != null
              && name.getContents() instanceof TranslatableContents contents
              && contents.getKey().equals(key),
          id + " default ITEM_NAME must use its translated description key " + key);
    }
    helper.assertTrue(items >= 506, "must cover the complete IC2 item registry, checked " + items);
    helper.assertTrue(blockItems >= 177, "must cover IC2 block items, checked " + blockItems);
    for (String key : english.keySet()) {
      helper.assertTrue(chinese.has(key), "Chinese language entry missing: " + key);
    }
    helper.succeed();
  }

  @GameTest(template = TEMPLATE, timeoutTicks = 20)
  public static void representativeBlockItemsUseTranslatedNames(GameTestHelper helper) {
    JsonObject chinese = readLanguage("zh_cn");
    var items =
        new net.minecraft.world.item.Item[] {
          Ic2Items.EXTRACTOR, Ic2Items.COPPER_CABLE, Ic2Items.BATBOX,
          Ic2Items.RUBBER_SIGN, Ic2Items.REINFORCED_DOOR, Ic2Items.CROP_STICK
        };
    for (var item : items) {
      var id = BuiltInRegistries.ITEM.getKey(item);
      String key = "block.ic2." + id.getPath();
      helper.assertTrue(item.getDescriptionId().equals(key), id + " must use " + key);
      String name = new ItemStack(item).getHoverName().getString();
      helper.assertTrue(!name.isEmpty() && !name.equals(key), id + " name must resolve: " + name);
    }
    helper.assertTrue(
        new ItemStack(Ic2Items.EXTRACTOR).getHoverName().getString().equals("Extractor"),
        "extractor must resolve to its English name on the server");
    helper.assertTrue(
        chinese.get(Ic2Items.EXTRACTOR.getDescriptionId()).getAsString().equals("提取机"),
        "extractor must resolve to its existing Chinese name");
    helper.succeed();
  }

  private static JsonObject readLanguage(String language) {
    String path = "/assets/ic2/lang/" + language + ".json";
    try (var stream = TranslationGameTests.class.getResourceAsStream(path)) {
      if (stream == null) throw new IllegalStateException("Missing language resource " + path);
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (java.io.IOException exception) {
      throw new RuntimeException("Cannot read " + path, exception);
    }
  }

  private static void assertTranslated(GameTestHelper helper, Entity entity, String key) {
    String name = entity.getName().getString();
    helper.assertTrue(
        !name.isEmpty() && !name.equals(key),
        "entity name for " + key + " must be translated, got \"" + name + "\"");
  }
}
