package twilightforest.client.renderer;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import java.util.List;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OitRenderRegressionTests {
    @Test void everyCustomTranslucentRenderTypeProvidesAllNativeOitStages() throws Exception {
        var stateField = RenderType.class.getDeclaredField("state"); stateField.setAccessible(true);
        var oitField = RenderSetup.class.getDeclaredField("oitPipelineSet"); oitField.setAccessible(true);
        for (var type : List.of(TFRenderTypes.RED_THREAD, TFRenderTypes.PROTECTION_BOX, TFRenderTypes.SHADOW_CLONE)) {
            assertTrue(type.hasBlending());
            var stages = (OitPipelineSet) oitField.get(stateField.get(type));
            assertNotNull(stages, type.toString());
            for (var stage : OitStage.values()) {
                var pipeline = stages.getPipeline(stage);
                assertNotNull(pipeline);
                assertTrue(TFRenderPipelines.PIPELINES.contains(pipeline));
                assertSame(type.format(), pipeline.getVertexFormatBinding(0));
                assertDoesNotThrow(() -> BindGroupLayout.ensureCompatible(pipeline.getBindGroupLayouts()));
            }
        }
    }

    @Test void redThreadRemainsAdditiveAndVisibleThroughOpaqueTerrainInEveryStage() {
        for (var stage : OitStage.values()) {
            var pipeline = TFRenderPipelines.OIT_RED_THREAD.getPipeline(stage);
            assertNull(pipeline.getDepthStencilState());
            assertTrue(pipeline.getShaderDefines().flags().contains("OIT_ADDITIVE"));
        }
    }

    @Test void shieldKeepsTextureAnimationAndCloneKeepsOverlayAndLighting() {
        for (var stage : OitStage.values()) {
            var shield = TFRenderPipelines.OIT_PROTECTION_BOX.getPipeline(stage);
            assertTrue(shield.getShaderDefines().flags().containsAll(List.of("EMISSIVE", "APPLY_TEXTURE_MATRIX", "NO_OVERLAY", "NO_CARDINAL_LIGHTING")));
            assertFalse(shield.getShaderDefines().flags().contains("OIT_ADDITIVE"));
        }
        var clone = TFRenderPipelines.OIT_SHADOW_CLONE.accumulatePipeline();
        var names = BindGroupLayout.flattenUniforms(clone.getBindGroupLayouts()).stream().map(BindGroupLayout.UniformDescription::name).toList();
        assertTrue(names.containsAll(List.of("Sampler0", "Sampler1", "Sampler2", "Lighting")));
        assertFalse(clone.isCull());
        assertEquals(TFRenderPipelines.PIPELINES.size(), TFRenderPipelines.PIPELINES.stream().map(p -> p.getLocation()).distinct().count());
    }
}
