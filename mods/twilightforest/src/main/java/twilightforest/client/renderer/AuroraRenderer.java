package twilightforest.client.renderer;

import com.google.common.base.Suppliers;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicGpuDataStorage;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;

public class AuroraRenderer {

	private final Supplier<DynamicGpuDataStorageMapped<AuroraUniform>> uniformStorage = Suppliers.memoize(() ->
		new DynamicGpuDataStorageMapped<>("Twilight Forest Aurora UBO", AuroraUniform.SIZE, GpuBuffer.USAGE_UNIFORM, 2)
	);

	public void endFrame() {
		this.uniformStorage.get().endFrame();
	}

	public void draw(MeshData mesh, float alpha, int seed, float x, float y, float z) {
		GpuDevice device = RenderSystem.getDevice();
		GpuTextureView colorTexture = Minecraft.getInstance().gameRenderer.mainRenderTarget().getColorTextureView();
		GpuTextureView depthTexture = Minecraft.getInstance().gameRenderer.mainRenderTarget().getDepthTextureView();

		try (mesh) {
			if (colorTexture == null)
				return;

			MeshData.DrawState drawState = mesh.drawState();
			RenderSystem.AutoStorageIndexBuffer sequential = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
			GpuBuffer indexBuffer = sequential.getBuffer(drawState.indexCount());
			GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrixCopy(), new Vector4f(1.0F, 1.0F, 1.0F, alpha), new Vector3f(), new Matrix4f());
			GpuBufferSlice aurora = this.uniformStorage.get().writeData(new AuroraUniform(seed, x, y, z));

			try (
				GpuBuffer vertexBuffer = device.createBuffer(
					() -> "Twilight Forest aurora vertices",
					GpuBuffer.USAGE_VERTEX,
					mesh.vertexBuffer()
				);
				RenderPass pass = device.createCommandEncoder().createRenderPass(
					() -> "Aurora",
					colorTexture,
					java.util.Optional.empty(),
					depthTexture,
					OptionalDouble.empty()
				)
			) {
				pass.setPipeline(RenderSystem.getCompiledPipeline(TFRenderPipelines.AURORA));
				RenderSystem.bindDefaultUniforms(pass);
				pass.setUniform("DynamicTransforms", transforms);
				pass.setUniform(TFRenderPipelines.AURORA_UNIFORM, aurora);
				pass.setVertexBuffer(0, vertexBuffer.slice());
				pass.setIndexBuffer(indexBuffer, sequential.type());
				pass.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
			}
		}
	}

	private record AuroraUniform(int seed, float x, float y, float z) implements DynamicGpuDataStorage.DynamicGpuData {

		private static final int SIZE = new Std140SizeCalculator().putInt().putVec3().align(16).get();

		@Override
		public void write(ByteBuffer buffer) {
			Std140Builder.intoBuffer(buffer).putInt(this.seed).putVec3(this.x, this.y, this.z).align(16);
		}

	}

}
