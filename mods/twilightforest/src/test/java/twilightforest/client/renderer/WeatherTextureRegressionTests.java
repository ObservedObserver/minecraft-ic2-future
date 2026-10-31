package twilightforest.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.commands.RenderPass;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tamaized.beanification.junit.MockitoFixer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoFixer.class)
class WeatherTextureRegressionTests {
    @SuppressWarnings("try")
    @Test void coldAndReloadedWeatherTexturesAreResolvedBeforeAnyRenderPass() throws Exception {
        var id = Identifier.fromNamespaceAndPath("twilightforest", "textures/environment/sparkles.png");
        var vertex = new WeatherGeometry.Vertex(0, 0, 0, 0, 0, -1, 0xF000F0);
        var geometry = new WeatherGeometry(Map.of(id, List.of(vertex, vertex, vertex, vertex)));
        var passOpen = new AtomicBoolean();
        var textures = mock(TextureManager.class);
        var firstTexture = mock(AbstractTexture.class, RETURNS_DEEP_STUBS);
        var reloadedTexture = mock(AbstractTexture.class, RETURNS_DEEP_STUBS);
        var currentTexture = new java.util.concurrent.atomic.AtomicReference<>(firstTexture);
        when(textures.getTexture(id)).thenAnswer(call -> {
            assertFalse(passOpen.get(), "A cold texture upload must not happen inside a render pass");
            return currentTexture.get();
        });
        var minecraft = mock(Minecraft.class);
        when(minecraft.getTextureManager()).thenReturn(textures);
        var rendererField = Minecraft.class.getDeclaredField("gameRenderer");
        rendererField.setAccessible(true);
        rendererField.set(minecraft, mock(GameRenderer.class, RETURNS_DEEP_STUBS));
        var mesh = mock(MeshData.class);
        when(mesh.vertexBuffer()).thenReturn(ByteBuffer.allocate(128));
        try (var mc = mockStatic(Minecraft.class);
             var render = mockStatic(RenderSystem.class, RETURNS_DEEP_STUBS);
             var bytes = mockStatic(ByteBufferBuilder.class);
             var builders = mockConstruction(BufferBuilder.class, withSettings().defaultAnswer(RETURNS_SELF),
                 (builder, context) -> when(builder.buildOrThrow()).thenReturn(mesh));
             var weather = new WeatherGeometryRenderer()) {
            mc.when(Minecraft::getInstance).thenReturn(minecraft);
            bytes.when(() -> ByteBufferBuilder.exactlySized(anyInt())).thenReturn(mock(ByteBufferBuilder.class));
            for (var expected : List.of(firstTexture, reloadedTexture)) {
                currentTexture.set(expected);
                passOpen.set(false);
                weather.prepare(geometry);
                clearInvocations(textures);
                var pass = mock(RenderPass.class);
                passOpen.set(true);
                weather.render(pass, null);
                for (var stage : OitStage.values()) weather.render(pass, stage);
                verifyNoInteractions(textures);
                verify(pass, times(4)).setUniform("Sampler0", expected.getTextureView(), expected.getSampler());
            }
            passOpen.set(false);
            weather.prepare(WeatherGeometry.EMPTY);
            var emptyPass = mock(RenderPass.class);
            weather.render(emptyPass, OitStage.values()[0]);
            verifyNoInteractions(emptyPass);
        }
    }
}
