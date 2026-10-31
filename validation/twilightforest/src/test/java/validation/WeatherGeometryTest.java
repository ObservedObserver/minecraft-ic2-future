package validation;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import twilightforest.client.renderer.WeatherGeometry;
import static org.junit.jupiter.api.Assertions.*;

class WeatherGeometryTest {
	private static Identifier id(String value) { return Identifier.fromNamespaceAndPath("twilightforest", value); }
	private static void quad(VertexConsumer consumer, int color, int light) {
		for (int i = 0; i < 4; i++) consumer.addVertex(i - 2.5F, i * 0.25F, -i).setUv(i * 0.1F, -i * 0.5F).setColor(color).setLight(light);
	}

	@Test void immutableSnapshotRetainsTextureOrderAndExactParticleAttributes() {
		var builder = new WeatherGeometry.Builder();
		quad(builder.getBuffer(id("blizzard")), 0xAABC1234, 0x00E00080);
		quad(builder.getBuffer(id("rain")), 0xCF345678, 0x00F00070);
		var first = builder.build();
		assertEquals(List.of(id("blizzard"), id("rain")), List.copyOf(first.batches().keySet()));
		var vertex = first.batches().get(id("blizzard")).get(2);
		assertEquals(new WeatherGeometry.Vertex(-0.5F, 0.5F, -2, 0.2F, -1, 0xAABC1234, 0x00E00080), vertex);
		quad(builder.getBuffer(id("blizzard")), -1, 0);
		assertEquals(4, first.batches().get(id("blizzard")).size());
		assertEquals(8, builder.build().batches().get(id("blizzard")).size());
		assertThrows(UnsupportedOperationException.class, () -> first.batches().clear());
		assertThrows(UnsupportedOperationException.class, () -> first.batches().get(id("rain")).clear());
	}

	@Test void floatColorsUseNativeByteQuantizationAndPackedLightKeepsBothChannels() {
		var builder = new WeatherGeometry.Builder(); var consumer = builder.getBuffer(id("mosquitoes"));
		for (int i = 0; i < 4; i++) consumer.addVertex(0, 1, 2).setUv(3, 4).setColor(0.25F, 0.5F, 0.75F, 1).setUv2(240, 128);
		var vertex = builder.build().batches().get(id("mosquitoes")).getFirst();
		assertEquals(0xFF3F7FBF, vertex.color()); assertEquals(0x008000F0, vertex.light());
	}

	@Test void rejectsPartialQuadsAndMissingAttributesBeforeGpuUpload() {
		var incomplete = new WeatherGeometry.Builder();
		incomplete.getBuffer(id("rain")).addVertex(1, 2, 3).setUv(0, 0).setColor(-1).setLight(0);
		assertThrows(IllegalStateException.class, incomplete::build);
		var missing = new WeatherGeometry.Builder();
		missing.getBuffer(id("rain")).addVertex(1, 2, 3).setColor(-1);
		assertThrows(IllegalStateException.class, missing::build);
	}

	@Test void uploadConsumerReceivesUnchangedPositionUvColorAndLight() {
		List<String> methods = new ArrayList<>(); List<Object[]> args = new ArrayList<>();
		var consumer = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(), new Class<?>[]{VertexConsumer.class},
			(proxy, method, values) -> { methods.add(method.getName()); args.add(values); return proxy; });
		new WeatherGeometry.Vertex(-1.5F, 20, -3, 2.25F, -7.75F, 0xCC112233, 0x00F00020).write(consumer);
		assertEquals(List.of("addVertex", "setUv", "setColor", "setLight"), methods);
		assertArrayEquals(new Object[]{-1.5F, 20F, -3F}, args.get(0));
		assertArrayEquals(new Object[]{2.25F, -7.75F}, args.get(1));
		assertEquals(0xCC112233, args.get(2)[0]); assertEquals(0x00F00020, args.get(3)[0]);
	}
}
