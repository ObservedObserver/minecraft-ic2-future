package twilightforest.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

/** Immutable, CPU-only weather vertices extracted before the render pass begins. */
public record WeatherGeometry(Map<Identifier, List<Vertex>> batches) {
	public static final WeatherGeometry EMPTY = new WeatherGeometry(Map.of());

	public record Vertex(float x, float y, float z, float u, float v, int color, int light) {
		public void write(VertexConsumer consumer) {
			consumer.addVertex(x, y, z).setUv(u, v).setColor(color).setLight(light);
		}
	}

	public static final class Builder {
		private final Map<Identifier, ParticleConsumer> consumers = new LinkedHashMap<>();

		public VertexConsumer getBuffer(Identifier texture) {
			return consumers.computeIfAbsent(texture, _ -> new ParticleConsumer());
		}

		public WeatherGeometry build() {
			Map<Identifier, List<Vertex>> result = new LinkedHashMap<>();
			consumers.forEach((texture, consumer) -> {
				consumer.finishVertex();
				if (consumer.vertices.size() % 4 != 0) throw new IllegalStateException("Incomplete weather quad");
				if (!consumer.vertices.isEmpty()) result.put(texture, List.copyOf(consumer.vertices));
			});
			return new WeatherGeometry(java.util.Collections.unmodifiableMap(result));
		}
	}

	private static final class ParticleConsumer implements VertexConsumer {
		private final List<Vertex> vertices = new ArrayList<>();
		private float x, y, z, u, v;
		private int color, light, attributes;
		private boolean pending;

		private void finishVertex() {
			if (!pending) return;
			if (attributes != 7) throw new IllegalStateException("Weather vertex lacks UV, color or light");
			vertices.add(new Vertex(x, y, z, u, v, color, light));
			pending = false;
		}

		@Override public VertexConsumer addVertex(float x, float y, float z) {
			finishVertex(); this.x = x; this.y = y; this.z = z; pending = true; attributes = 0; return this;
		}
		@Override public VertexConsumer setUv(float u, float v) { this.u = u; this.v = v; attributes |= 1; return this; }
		@Override public VertexConsumer setColor(int color) { this.color = color; attributes |= 2; return this; }
		@Override public VertexConsumer setColor(int r, int g, int b, int a) {
			return setColor((a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255);
		}
		@Override public VertexConsumer setUv2(int u, int v) { light = (u & 65535) | (v & 65535) << 16; attributes |= 4; return this; }
		@Override public VertexConsumer setUv1(int u, int v) { throw unsupported(); }
		@Override public VertexConsumer setUv3(float u, float v) { throw unsupported(); }
		@Override public VertexConsumer setNormal(float x, float y, float z) { throw unsupported(); }
		@Override public VertexConsumer setLineWidth(float width) { throw unsupported(); }
		private IllegalStateException unsupported() { return new IllegalStateException("Attribute is not part of the native PARTICLE format"); }
	}
}
