package validation;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import org.junit.jupiter.api.Test;
import twilightforest.client.renderer.ModelSubmission;
import static org.junit.jupiter.api.Assertions.*;

class ModelSubmissionTest {
	private record Call(String method, Object[] args) {}
	private final List<Call> calls = new ArrayList<>();
	private final OrderedSubmitNodeCollector collector = (OrderedSubmitNodeCollector) Proxy.newProxyInstance(
		OrderedSubmitNodeCollector.class.getClassLoader(), new Class<?>[]{OrderedSubmitNodeCollector.class},
		(proxy, method, args) -> { calls.add(new Call(method.getName(), args)); return null; });
	private final PoseStack pose = new PoseStack();
	private final ModelPart part = new ModelPart(List.of(), Map.of());
	private final net.minecraft.client.renderer.rendertype.RenderType renderType =
		RenderTypes.entityCutout(Identifier.fromNamespaceAndPath("twilightforest", "textures/model/test.png"));
	private final Model.Simple model = new Model.Simple(part, texture -> renderType);

	@Test
	void intactModelRetainsOutlineAndDoesNotSubmitDestruction() {
		ModelSubmission.submit(collector, model, Unit.INSTANCE, pose, renderType, 934, 281, 0xFF771133, null);
		assertEquals(1, calls.size());
		var call = calls.getFirst();
		assertEquals("submitModel", call.method());
		assertSame(model, call.args()[0]); assertSame(Unit.INSTANCE, call.args()[1]);
		assertSame(pose, call.args()[2]); assertSame(renderType, call.args()[3]);
		assertEquals(934, call.args()[4]); assertEquals(281, call.args()[5]);
		assertEquals(-1, call.args()[6]); assertNull(call.args()[7]);
		assertEquals(0xFF771133, call.args()[8]);
	}

	@Test
	void damagedModelSubmitsBothLayersWithSameStatePoseAndTint() {
		var destruction = new ModelFeatureRenderer.CrumblingOverlay(7, pose.last().copy());
		ModelSubmission.submit(collector, model, Unit.INSTANCE, pose, renderType, 189, 823, 0xFF123456, null, 992, destruction);
		assertEquals(List.of("submitModel", "submitCrumblingOverlay"), calls.stream().map(Call::method).toList());
		var base = calls.get(0).args(); var overlay = calls.get(1).args();
		for (int i = 0; i < 7; i++) assertEquals(base[i], overlay[i]);
		assertEquals(992, base[8]); assertSame(destruction, overlay[7]);
	}

	@Test
	void individualPartsRetainDestructionPassAndColor() {
		var destruction = new ModelFeatureRenderer.CrumblingOverlay(9, pose.last().copy());
		ModelSubmission.submitPart(collector, part, pose, renderType, 129, 53, null, 0xA0123456, destruction);
		assertEquals(List.of("submitModelPart", "submitCrumblingOverlay"), calls.stream().map(Call::method).toList());
		assertSame(part, calls.get(0).args()[0]); assertSame(part, calls.get(1).args()[0]);
		assertEquals(0xA0123456, calls.get(0).args()[6]);
		assertEquals(0xA0123456, calls.get(1).args()[5]);
		assertSame(destruction, calls.get(1).args()[6]);
	}
}
