package ic2.core.gametest;

import ic2.compat.GameTest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registers every existing IC2 test using the 26.3 function/test registries. */
public final class Ic2GameTests {
  private Ic2GameTests() {}

  private static final Class<?>[] SUITE = {
    ic2.core.energy.grid.EnergyCalculatorUnifiedGameTests.class,
    ActiveStateGameTests.class,
    AdvMinerGameTests.class,
    AirCellGameTests.class,
    ArmorGameTests.class,
    CableGameTests.class,
    ChainsawGameTests.class,
    ChargePadGameTests.class,
    ChunkLoaderGameTests.class,
    CokeKilnGameTests.class,
    CraftingMachineGameTests.class,
    CropGameTests.class,
    CropGroundGameTests.class,
    DrillGameTests.class,
    ElectricHoeGameTests.class,
    ElectricWrenchGameTests.class,
    EnergyNetGameTests.class,
    ExplosiveGameTests.class,
    FluidMachineGameTests.class,
    FluidPlacementGameTests.class,
    FluidRoutingGameTests.class,
    FoamSprayerGameTests.class,
    FrequencyTransmitterGameTests.class,
    GeneratorGameTests.class,
    GradualRecipeGameTests.class,
    GuiParserGameTests.class,
    IridiumLootGameTests.class,
    TwilightForestLootGameTests.class,
    ItemRoutingGameTests.class,
    JetpackGameTests.class,
    KineticGeneratorGameTests.class,
    MachineGameTests.class,
    MachineLootGameTests.class,
    ManualFluidTransferGameTests.class,
    MagnetizerGameTests.class,
    MatterGameTests.class,
    MeterGameTests.class,
    MinerGameTests.class,
    MiningLaserGameTests.class,
    NanoSaberGameTests.class,
    NanoSuitGameTests.class,
    ObscuratorGameTests.class,
    PainterGameTests.class,
    PersonalSafeGameTests.class,
    ProcessingMachineGameTests.class,
    PumpGameTests.class,
    QuantumSuitGameTests.class,
    ReactorGameTests.class,
    RubberLeavesGameTests.class,
    ScrapBoxGameTests.class,
    SteamBoilerCalcificationGameTests.class,
    StorageBoxGameTests.class,
    SulfurIntegrationGameTests.class,
    RecipeExpansionGameTests.class,
    TeleporterGameTests.class,
    TerraformerGameTests.class,
    TeslaCoilGameTests.class,
    ToolboxGameTests.class,
    TranslationGameTests.class,
    TreetapGameTests.class,
    NativeEffectHookGameTests.class,
    NativeEntityHookGameTests.class,
    NativeBlockHookGameTests.class,
    UpgradeGameTests.class,
    WindGameTests.class
  };

  private static Identifier id(Class<?> owner, Method method) {
    return Identifier.fromNamespaceAndPath(
        "ic2", (owner.getSimpleName() + "." + method.getName()).toLowerCase(Locale.ROOT));
  }

  public static void registerFunctions(RegisterEvent event) {
    if (event.getRegistryKey() != Registries.TEST_FUNCTION) return;
    for (Class<?> owner : SUITE) {
      for (Method method : owner.getDeclaredMethods()) {
        if (!method.isAnnotationPresent(GameTest.class)) continue;
        Consumer<GameTestHelper> function = helper -> invoke(method, helper);
        event.register(Registries.TEST_FUNCTION, id(owner, method), () -> function);
      }
    }
  }

  public static void registerTests(RegisterGameTestsEvent event) {
    Map<String, Holder<TestEnvironmentDefinition<?>>> batches = new HashMap<>();
    for (Class<?> owner : SUITE) {
      for (Method method : owner.getDeclaredMethods()) {
        GameTest annotation = method.getAnnotation(GameTest.class);
        if (annotation == null) continue;
        Holder<TestEnvironmentDefinition<?>> environment =
            batches.computeIfAbsent(
                annotation.batch(),
                batch ->
                    event.registerEnvironment(
                        Identifier.fromNamespaceAndPath("ic2", batch.toLowerCase(Locale.ROOT)),
                        new TestEnvironmentDefinition.AllOf(java.util.List.of())));
        Identifier testId = id(owner, method);
        Identifier structure =
            annotation.template().contains(":")
                ? Identifier.parse(annotation.template())
                : Identifier.fromNamespaceAndPath("ic2", annotation.template());
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
            new TestData<>(
                environment,
                structure,
                annotation.timeoutTicks(),
                annotation.setupTicks(),
                annotation.required());
        event.registerTest(
            testId,
            new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, testId), data));
      }
    }
  }

  private static void invoke(Method method, GameTestHelper helper) {
    try {
      method.invoke(null, helper);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new RuntimeException("IC2 GameTest failed: " + method, cause);
    } catch (ReflectiveOperationException exception) {
      throw new RuntimeException("Cannot invoke IC2 GameTest: " + method, exception);
    }
  }
}
