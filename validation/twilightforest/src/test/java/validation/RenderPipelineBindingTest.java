package validation;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.*;
import org.junit.jupiter.api.Test;
import twilightforest.client.renderer.AuroraRenderer;
import twilightforest.client.renderer.TFRenderPipelines;
import static org.junit.jupiter.api.Assertions.*;

class RenderPipelineBindingTest {
    @Test
    void allMigratedPipelinesHaveCompatibleUniqueBindings() {
        for (RenderPipeline pipeline : List.of(TFRenderPipelines.AURORA, TFRenderPipelines.RED_THREAD,
                TFRenderPipelines.PROTECTION_BOX, TFRenderPipelines.SHADOW_CLONE)) {
            assertDoesNotThrow(() -> BindGroupLayout.ensureCompatible(pipeline.getBindGroupLayouts()));
            assertEquals("twilightforest", pipeline.getLocation().getNamespace());
        }
    }

    @Test
    void auroraKeepsTransformsFogGlobalsAndCustomUniformBuffer() {
        var pipeline = TFRenderPipelines.AURORA;
        var uniforms = BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());
        var names = uniforms.stream().map(BindGroupLayout.UniformDescription::name).toList();
        assertTrue(names.containsAll(List.of("DynamicTransforms", "Projection", "Fog", "Globals", TFRenderPipelines.AURORA_UNIFORM)));
        assertEquals(1, names.stream().filter(TFRenderPipelines.AURORA_UNIFORM::equals).count());
        assertEquals(UniformType.UNIFORM_BUFFER, uniforms.stream().filter(u -> u.name().equals(TFRenderPipelines.AURORA_UNIFORM)).findFirst().orElseThrow().type());
        assertEquals(PrimitiveTopology.QUADS, pipeline.getPrimitiveTopology());
        assertSame(DefaultVertexFormat.POSITION_COLOR, pipeline.getVertexFormatBinding(0));
    }

    @Test
    void redThreadAndShadowKeepTheirTextureAndLightingBindings() {
        var red = BindGroupLayout.flattenUniforms(TFRenderPipelines.RED_THREAD.getBindGroupLayouts()).stream().map(BindGroupLayout.UniformDescription::name).toList();
        assertTrue(red.containsAll(List.of("Sampler0", "Sampler2")));
        var shadow = BindGroupLayout.flattenUniforms(TFRenderPipelines.SHADOW_CLONE.getBindGroupLayouts()).stream().map(BindGroupLayout.UniformDescription::name).toList();
        assertTrue(shadow.containsAll(List.of("Sampler0", "Sampler1", "Sampler2", "Lighting")));
    }

    @Test
    void auroraStd140PayloadMatchesTheShadersIntThenAlignedVec3() throws Exception {
        var type = java.util.Arrays.stream(AuroraRenderer.class.getDeclaredClasses()).filter(c -> c.getSimpleName().equals("AuroraUniform")).findFirst().orElseThrow();
        var constructor = type.getDeclaredConstructor(int.class, float.class, float.class, float.class);
        constructor.setAccessible(true);
        var value = constructor.newInstance(73821, 1.25F, -12.5F, 332.75F);
        var size = type.getDeclaredField("SIZE"); size.setAccessible(true);
        assertEquals(32, size.getInt(null));
        var buffer = ByteBuffer.allocate(32).order(ByteOrder.nativeOrder());
        var write = type.getDeclaredMethod("write", ByteBuffer.class); write.setAccessible(true); write.invoke(value, buffer);
        assertEquals(73821, buffer.getInt(0));
        assertEquals(1.25F, buffer.getFloat(16)); assertEquals(-12.5F, buffer.getFloat(20)); assertEquals(332.75F, buffer.getFloat(24));
    }
}
