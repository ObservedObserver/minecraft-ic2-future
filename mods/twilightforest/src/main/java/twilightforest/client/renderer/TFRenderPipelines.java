package twilightforest.client.renderer;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Optional;
import java.util.List;
import net.minecraft.client.renderer.oit.OitPipelineSet;

public class TFRenderPipelines {
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("twilightforest", path); }

	public static final String AURORA_UNIFORM = "TwilightForestAurora";

	private static final BlendFunction SHADOW = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA);

	public static final RenderPipeline AURORA = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
		.withLocation(id("pipeline/aurora"))
		.withVertexShader(id("core/aurora/aurora"))
		.withFragmentShader(id("core/aurora/aurora"))
		.withBindGroupLayout(BindGroupLayout.builder().withUniform(AURORA_UNIFORM, UniformType.UNIFORM_BUFFER).build())
		.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
		.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.QUADS)
		.withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
		.build();

	private static final RenderPipeline.Snippet RED_THREAD_SNIPPET = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
		.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
		.withBindGroupLayout(BindGroupLayouts.SAMPLER2)
		.withVertexShader("core/block")
		.withFragmentShader("core/block")
		.withVertexBinding(0, DefaultVertexFormat.BLOCK).withPrimitiveTopology(PrimitiveTopology.QUADS)
		.withCull(true)
		.buildSnippet();

	public static final RenderPipeline RED_THREAD = RenderPipeline.builder(RED_THREAD_SNIPPET)
		.withLocation(id("core/red_thread/red_thread"))
		.withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
		.withDepthStencilState(Optional.of(new DepthStencilState(CompareOp.ALWAYS_PASS, false)))
		.build();

	public static final RenderPipeline PROTECTION_BOX = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
		.withLocation(id("pipeline/protection_box"))
		.withVertexShader("core/entity")
		.withFragmentShader("core/entity")
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withShaderDefine("EMISSIVE")
		.withShaderDefine("NO_OVERLAY")
		.withShaderDefine("NO_CARDINAL_LIGHTING")
		.withShaderDefine("APPLY_TEXTURE_MATRIX")
		.withBindGroupLayout(BindGroupLayouts.SAMPLER0)
		.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
		.withCull(false)
		.withVertexBinding(0, DefaultVertexFormat.ENTITY).withPrimitiveTopology(PrimitiveTopology.QUADS)
		.withDepthStencilState(DepthStencilState.DEFAULT)
		.build();

	public static final RenderPipeline SHADOW_CLONE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
		.withLocation(id("pipeline/shadow_clone"))
		.withShaderDefine("ALPHA_CUTOUT", 0.1F)
		.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
		.withCull(false)
		.withColorTargetState(new ColorTargetState(SHADOW))
		.build();

	public static final OitPipelineSet OIT_RED_THREAD = OitPipelineSet.builder(id("red_thread"),
		RenderPipeline.builder(RED_THREAD_SNIPPET)
			.withFragmentShader(id("core/red_thread/oit_red_thread"))
			.withShaderDefine("OIT_ADDITIVE"))
		// Highlighted thread remains visible through terrain in every OIT stage.
		.withoutDepthTest()
		.build();

	public static final OitPipelineSet OIT_PROTECTION_BOX = OitPipelineSet.builder(id("protection_box"),
		RenderPipeline.builder(RenderPipelines.ENERGY_SWIRL_SNIPPET))
		// Same emissive, animated texture as energy swirl, but ordinary translucency.
		.build();

	public static final OitPipelineSet OIT_SHADOW_CLONE = OitPipelineSet.builder(id("shadow_clone"),
		RenderPipeline.builder(RenderPipelines.OIT_ENTITY_SNIPPET).withCull(false))
		.withAccumulateModifier(builder -> builder.withBindGroupLayout(BindGroupLayouts.SAMPLER1)
			.withBindGroupLayout(BindGroupLayouts.SAMPLER2))
		.build();

	public static final List<RenderPipeline> PIPELINES = List.of(
		AURORA, RED_THREAD, PROTECTION_BOX, SHADOW_CLONE,
		OIT_RED_THREAD.depthBoundsPipeline(), OIT_RED_THREAD.transmittancePipeline(), OIT_RED_THREAD.accumulatePipeline(),
		OIT_PROTECTION_BOX.depthBoundsPipeline(), OIT_PROTECTION_BOX.transmittancePipeline(), OIT_PROTECTION_BOX.accumulatePipeline(),
		OIT_SHADOW_CLONE.depthBoundsPipeline(), OIT_SHADOW_CLONE.transmittancePipeline(), OIT_SHADOW_CLONE.accumulatePipeline());

}
