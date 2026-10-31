from pathlib import Path
p=Path('src/main/java/ic2/forge/EnvFluidHandlerForge.java');s=p.read_text();a=s.index('          FluidType fluidType =');b=s.index('          Registry.register(NeoForgeRegistries.FLUID_TYPES',a)
s=s[:a]+'''          FluidType fluidType = new FluidType(attributesBuilder);
          FLUID_VISUALS.put(fluidType, new FluidVisual(stillSpriteId,
              flowingSpriteId == null ? stillSpriteId : flowingSpriteId, color));
'''+s[b:]
a=s.index('class EnvFluidHandlerForge');b=s.index('{',a)+1;s=s[:b]+'''
  static final java.util.Map<FluidType, FluidVisual> FLUID_VISUALS = new java.util.concurrent.ConcurrentHashMap<>();
  record FluidVisual(Identifier still, Identifier flowing, int color) {}
'''+s[b:];p.write_text(s)
p=Path('src/main/java/ic2/forge/ClientModEventHandlerForge.java');s=p.read_text();a=s.index('  @SubscribeEvent\n  public void onClientSetup(');s=s[:a]+'''  @SubscribeEvent
  public void onFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
    net.minecraft.core.registries.BuiltInRegistries.FLUID.forEach(fluid -> {
      var visual = EnvFluidHandlerForge.FLUID_VISUALS.get(fluid.getFluidType());
      if (visual == null) return;
      boolean translucent = (visual.color() >>> 24) < 255;
      event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(
          new net.minecraft.client.resources.model.sprite.Material(visual.still(), translucent),
          new net.minecraft.client.resources.model.sprite.Material(visual.flowing(), translucent), null,
          net.neoforged.neoforge.client.fluid.FluidTintSources.constant(visual.color())), fluid);
    });
  }

  @SubscribeEvent
  public void onFluidExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
    EnvFluidHandlerForge.FLUID_VISUALS.forEach((type, visual) -> event.registerFluidType(
        new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions() {
          @Override public void modifyFogColor(net.minecraft.client.Camera camera, float partialTick,
              net.minecraft.client.multiplayer.ClientLevel level, int renderDistance, float darkenWorldAmount,
              org.joml.Vector4f color) {
            color.set(((visual.color() >>> 16) & 255) / 255.0F, ((visual.color() >>> 8) & 255) / 255.0F,
                (visual.color() & 255) / 255.0F, 1.0F);
          }
          @Override public void modifyFogRender(net.minecraft.client.Camera camera,
              net.minecraft.client.renderer.fog.environment.FogEnvironment environment,
              float renderDistance, float partialTick, net.minecraft.client.renderer.fog.FogData data) {
            data.environmentalStart = -8.0F;
            data.environmentalEnd = (float) (ic2.core.util.Util.map(Math.abs(type.getDensity()), 20000.0, 2.0) * 0.5);
          }
        }, type));
  }

'''+s[a:];p.write_text(s)
p=Path('src/main/java/ic2/forge/ClientEnvFluidHandlerForge.java');p.write_text('''package ic2.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;

public class ClientEnvFluidHandlerForge extends EnvFluidHandlerForge {
  private static FluidModel model(Fluid fluid) { return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluid.defaultFluidState()); }
  @Override public Identifier getStillSpriteId(Fluid fluid) { return model(fluid).stillMaterial().sprite().contents().name(); }
  @Override public Identifier getFlowingSpriteId(Fluid fluid) { return model(fluid).flowingMaterial().sprite().contents().name(); }
  @Override public int getColor(Fluid fluid) {
    var tint = model(fluid).fluidTintSource();
    return tint == null ? -1 : tint.color(fluid.defaultFluidState());
  }
}
''')
p=Path('src/main/java/ic2/core/proxy/SideProxyClient.java');s=p.read_text().replace('args.length > 0 ? SideProxyServer.getMessageComponents(args) : new Object[0]),\n          false)', 'args.length > 0 ? SideProxyServer.getMessageComponents(args) : new Object[0]))');p.write_text(s)
# The pre/post living-render and block-highlight hooks were empty in the baseline.
# Fog behavior is implemented above via the modern fluid extensions.
p=Path('src/main/java/ic2/forge/ClientEventHandlerForge.java');s=p.read_text();a=s.index('  @SubscribeEvent\n  public void livingEntityPreRender(');b=s.index('  @SubscribeEvent\n  public void onGuiCreate(',a);s=s[:a]+s[b:];s=s.replace('import com.mojang.blaze3d.shaders.FogShape;\n','');p.write_text(s)
p=Path('src/main/java/ic2/core/event/EventHandlerClient.java');s=p.read_text();a=s.index('    List<Block> fluidBlocks');b=s.index('    SideProxyClient.envProxy.registerModelPredicateProvider(',a);s=s[:a]+s[b:];a=s.index('  public static void livingEntityPreRender(');b=s.index('  public static float onSetupFogDensity(',a);s=s[:a]+s[b:];a=s.index('  // TODO\n');b=s.index('  public static void onGuiCreate(',a);s=s[:a]+s[b:];s=s.replace('import net.minecraft.client.renderer.MultiBufferSource;\n','');p.write_text(s)
