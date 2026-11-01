package sortingprobe;

@net.neoforged.fml.common.Mod("sortingprobe")
public final class ProbeMod {
  public ProbeMod(net.neoforged.bus.api.IEventBus bus,
      net.neoforged.fml.ModContainer container) {
    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(SortingRestartProbe::tick);
  }
}
