package ic2.forge;

import ic2.core.event.EventHandler;
import ic2.core.init.IC2ClientConfig;
import ic2.core.init.IC2Config;
import ic2.core.loot.Ic2LootNbtProviderTypes;
import ic2.core.ref.Ic2Fluids;
import ic2.integration.ae2.Ic2Ae2Plugin;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("ic2")
public final class FmlMod {

  private static final AtomicInteger loadState = new AtomicInteger();

  public static FmlMod instance;

  private final ModContainer modContainer;

  private List<Runnable> toRunAfterRegistryInit = new ArrayList<>();

  public FmlMod(IEventBus modEventBus, ModContainer modContainer) {
    instance = this;
    this.modContainer = modContainer;
    modEventBus.register(this);
    EnvProxyForge.blockEntityRegistry.register(modEventBus);
    EnvProxyForge.creativeTabRegistry.register(modEventBus);
    EnvProxyForge.entityRegistry.register(modEventBus);
    EnvProxyForge.screenHandlerRegistry.register(modEventBus);
    EnvProxyForge.statusEffectRegistry.register(modEventBus);
    EnvProxyForge.foliagePlacerRegistry.register(modEventBus);
    EnvProxyForge.recipeTypeRegistry.register(modEventBus);
    EnvProxyForge.recipeSerializerRegistry.register(modEventBus);
    EnvFluidHandlerForge.fluidRegistry.register(modEventBus);
    EnvFluidHandlerForge.fluidTypeRegistry.register(modEventBus);
    Ic2LootModifier.lootModifiersRegistry.register(modEventBus);
    if (FMLEnvironment.getDist().isClient()) {
      modEventBus.register(new ClientModEventHandlerForge());
      this.modContainer.registerConfig(ModConfig.Type.CLIENT, IC2ClientConfig.SPEC);
    }
    Ic2Fluids.init();
    this.modContainer.registerConfig(ModConfig.Type.LOCAL, IC2Config.SPEC);
    modEventBus.addListener(Ic2Capabilities::register);
    modEventBus.addListener(Ic2Network::register);
    modEventBus.addListener(ic2.core.gametest.Ic2GameTests::registerFunctions);
    modEventBus.addListener(ic2.core.gametest.Ic2GameTests::registerTests);
  }

  @SubscribeEvent
  public void load(FMLCommonSetupEvent event) {
    NeoForge.EVENT_BUS.register(new EventHandlerForge());
    NeoForge.EVENT_BUS.register(new Ic2Ae2Plugin.ForgeEventHandler());
    if (FMLEnvironment.getDist().isClient()) {
      NeoForge.EVENT_BUS.register(new ClientEventHandlerForge());
    }
    if (!loadState.compareAndSet(1, 2)) {
      throw new IllegalStateException();
    }
    ic2.core.init.MainConfig.ignoreInvalidRecipes = IC2Config.recipes.ignoreInvalidRecipes.get();
  }

  @SubscribeEvent
  public void init(FMLLoadCompleteEvent event) {
    if (!loadState.compareAndSet(2, 3)) {
      throw new IllegalStateException();
    }
    // Stack-dependent setup runs after native default components are bound to holders.
  }

  @SubscribeEvent
  public void modifyFuelComponents(
      net.neoforged.neoforge.event.ModifyDefaultComponentsEvent event) {
    EnvProxyForge.burnTimeRecord.forEach(
        (item, ticks) ->
            event.modify(
                item,
                (components, context, key) ->
                    components.set(
                        net.minecraft.core.component.DataComponents.COOKING_FUEL,
                        ic2.compat.FuelCompat.constant(ticks))));
  }

  @SubscribeEvent
  public void registerTickets(RegisterEvent event) {
    event.register(
        Registries.TICKET_TYPE,
        ic2.core.IC2.getIdentifier("chunk_loader"),
        () -> ic2.core.ChunkLoaderLogic.TICKET_TYPE);
  }

  @SubscribeEvent
  public void registerFluidTypes(RegisterEvent event) {
    if (event.getRegistryKey() == NeoForgeRegistries.Keys.FLUID_TYPES) {
      EnvFluidHandlerForge.registerPendingFluidTypes();
    }
  }

  @SubscribeEvent
  public void registerLootNbtProviders(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.LOOT_NBT_PROVIDER_TYPE) {
      Ic2LootNbtProviderTypes.init();
    }
  }

  @SubscribeEvent
  public void registerFluids(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.FLUID) {
      EnvFluidHandlerForge.registerPendingFluids();
    }
  }

  @SubscribeEvent
  public void registerItems(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.ITEM) {
      EnvProxyForge.registerPendingItems();
    }
  }

  @SubscribeEvent
  public void registerStatusEffects(RegisterEvent event) {
    // MOB_EFFECT registers before BLOCK, so potions must be created here rather than in
    // onInitEarly.
    if (event.getRegistryKey() == Registries.MOB_EFFECT) {
      ic2.core.init.BlocksItems.initPotions();
    }
  }

  @SubscribeEvent
  public void registerBlocks(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.BLOCK) {
      if (!loadState.compareAndSet(0, 1)) {
        throw new IllegalStateException();
      }
      EventHandler.onInitEarly();
    }
  }

  @SubscribeEvent
  public void registerGameEvents(RegisterEvent event) {
    if (event.getRegistryKey() == Registries.SOUND_EVENT) {
      EventHandler.onInitGameEvents();
    }
  }

  private boolean defaultComponentsBound;

  void initializeDefaultComponentDependents() {
    this.defaultComponentsBound = true;
    if (this.toRunAfterRegistryInit != null) {
      for (Runnable runnable : this.toRunAfterRegistryInit) {
        runnable.run();
      }
      this.toRunAfterRegistryInit = null;
    }
  }

  void runAfterRegistryInit(Runnable runnable) {
    if (this.defaultComponentsBound) {
      runnable.run();
    } else {
      this.toRunAfterRegistryInit.add(runnable);
    }
  }
}
