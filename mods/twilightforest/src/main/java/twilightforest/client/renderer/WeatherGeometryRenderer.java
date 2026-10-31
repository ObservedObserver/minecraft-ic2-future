package twilightforest.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.jspecify.annotations.Nullable;

/** Uploads once outside render passes; the same geometry is replayed for every OIT stage. */
public final class WeatherGeometryRenderer implements AutoCloseable {
	private record Draw(AbstractTexture texture, int startIndex, int indexCount) {}
	private final List<Draw> draws = new ArrayList<>();
	private @Nullable GpuBuffer vertexBuffer;

	public void prepare(WeatherGeometry geometry) {
		draws.clear();
		int vertices = geometry.batches().values().stream().mapToInt(List::size).sum();
		if (vertices == 0) { close(); return; }
		try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(vertices * DefaultVertexFormat.PARTICLE.getVertexSize())) {
			BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.PARTICLE);
			int startIndex = 0;
			for (var batch : geometry.batches().entrySet()) {
				batch.getValue().forEach(vertex -> vertex.write(builder));
				int count = batch.getValue().size() / 4 * 6;
				// getTexture may upload on first use or after reload; no render pass is open here.
				AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(batch.getKey());
				draws.add(new Draw(texture, startIndex, count));
				startIndex += count;
			}
			try (MeshData mesh = builder.buildOrThrow()) {
				int size = mesh.vertexBuffer().remaining();
				if (vertexBuffer == null || vertexBuffer.size() < size) {
					if (vertexBuffer != null) vertexBuffer.close();
					vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "Twilight Forest weather", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, size);
				}
				RenderSystem.getDevice().createCommandEncoder().writeToBuffer(vertexBuffer.slice(), mesh.vertexBuffer());
			}
			RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).requestIndexCount(startIndex);
		}
	}

	public void render(RenderPass pass, @Nullable OitStage stage) {
		if (vertexBuffer == null || draws.isEmpty()) return;
		var minecraft = Minecraft.getInstance();
		var indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
		pass.pushDebugGroup(() -> "Twilight Forest weather");
		pass.setPipeline(RenderSystem.getCompiledPipeline(stage == null ? RenderPipelines.WEATHER : RenderPipelines.OIT_WEATHER.getPipeline(stage)));
		RenderSystem.bindDefaultUniforms(pass);
		pass.setUniform("DynamicTransforms", RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy()));
		pass.setUniform("Sampler2", minecraft.gameRenderer.lightmap(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
		pass.setVertexBuffer(0, vertexBuffer.slice());
		pass.setIndexBuffer(indices.getBuffer(), indices.type());
		for (Draw draw : draws) {
			AbstractTexture texture = draw.texture();
			pass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
			pass.drawIndexed(draw.indexCount(), 1, draw.startIndex(), 0, 0);
		}
		pass.popDebugGroup();
	}

	@Override public void close() {
		draws.clear();
		if (vertexBuffer != null) { vertexBuffer.close(); vertexBuffer = null; }
	}
}
