package ic2.core.util;

import ic2.core.IC2;
import ic2.core.proxy.SideProxyClient;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyboardClient extends Keyboard {
  public static final KeyMapping.Category CATEGORY =
      new KeyMapping.Category(IC2.getIdentifier("controls"));
  public static final KeyMapping altKey =
      new KeyMapping(
          "ic2.keyboard.alt_key", com.mojang.blaze3d.platform.InputConstants.KEY_LALT, CATEGORY);
  public static final KeyMapping boostKey =
      new KeyMapping(
          "ic2.keyboard.boost_key",
          com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL,
          CATEGORY);
  public static final KeyMapping modeSwitchKey =
      new KeyMapping(
          "ic2.keyboard.mode_switch_key",
          com.mojang.blaze3d.platform.InputConstants.KEY_M,
          CATEGORY);
  public static final KeyMapping sideInventoryKey =
      new KeyMapping(
          "ic2.keyboard.side_inventory_key",
          com.mojang.blaze3d.platform.InputConstants.KEY_C,
          CATEGORY);
  public static final KeyMapping expandInfo =
      new KeyMapping(
          "ic2.keyboard.hub_expand_key",
          com.mojang.blaze3d.platform.InputConstants.KEY_X,
          CATEGORY);
  private static boolean registeredKeys = false;
  private int lastKeyState = 0;

  public KeyboardClient() {
    if (!registeredKeys) {
      registeredKeys = true;
      SideProxyClient.envProxy.registerKeyBinding(this.altKey);
      SideProxyClient.envProxy.registerKeyBinding(this.boostKey);
      SideProxyClient.envProxy.registerKeyBinding(this.modeSwitchKey);
      SideProxyClient.envProxy.registerKeyBinding(this.sideInventoryKey);
      SideProxyClient.envProxy.registerKeyBinding(this.expandInfo);
    }
  }

  @Override
  public void sendKeyUpdate() {
    // NeoForge constructs the client proxy before Minecraft's singleton is assigned.
    // Resolve it at tick time so an early null is never retained for the whole session.
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.gui == null || mc.options == null) {
      return;
    }
    Set<Keyboard.Key> keys = EnumSet.noneOf(Keyboard.Key.class);
    Screen currentScreen = mc.gui.screen();
    if (currentScreen == null) {
      if (this.altKey.isDown()) {
        keys.add(Keyboard.Key.alt);
      }

      if (this.boostKey.isDown()) {
        keys.add(Keyboard.Key.boost);
      }

      if (mc.options.keyUp.isDown()) {
        keys.add(Keyboard.Key.forward);
      }

      if (this.modeSwitchKey.isDown()) {
        keys.add(Keyboard.Key.modeSwitch);
      }

      if (mc.options.keyJump.isDown()) {
        keys.add(Keyboard.Key.jump);
      }

      if (this.sideInventoryKey.isDown()) {
        keys.add(Keyboard.Key.sideInventory);
      }

      if (this.expandInfo.isDown()) {
        keys.add(Keyboard.Key.hubMode);
      }

      for (Keyboard.IKeyWatcher watcher : this.watchers) {
        watcher.checkForKey(keys);
      }
    }

    int currentKeyState = Keyboard.Key.toInt(keys);
    if (currentKeyState != this.lastKeyState) {
      IC2.network.get(false).initiateKeyUpdate(currentKeyState);
      super.processKeyUpdate(IC2.sideProxy.getPlayerInstance(), currentKeyState);
      this.lastKeyState = currentKeyState;
    }
  }
}
