package validation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import twilightforest.asm.transformers.render.HideHeadUnderTrophyTransformer;
import static org.junit.jupiter.api.Assertions.*;

class HeadVisibilityTransformerTest {
	@Test void targetsActualNativePreparationAndRestoresAfterRendering() throws Exception {
		var clazz = new ClassNode();
		try (var input = ModelFeatureRenderer.class.getResourceAsStream("ModelFeatureRenderer.class")) {
			assertNotNull(input); new ClassReader(input).accept(clazz, 0);
		}
		var method = clazz.methods.stream().filter(m -> m.name.equals("prepareModel")).findFirst().orElseThrow();
		assertEquals("(Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit;)V", method.desc);
		new HideHeadUnderTrophyTransformer().transform(method, null);
		List<String> relevantCalls = new ArrayList<>();
		for (var insn : method.instructions) {
			if (insn instanceof MethodInsnNode call && (call.name.equals("setupAnim") || call.name.equals("renderToBuffer")
				|| call.owner.equals("twilightforest/asmhooks/RenderHooks"))) relevantCalls.add(call.name);
		}
		assertEquals(List.of("setupAnim", "applyHeadVisibility", "renderToBuffer", "restoreHeadVisibility"), relevantCalls);
		new Analyzer<>(new BasicVerifier()).analyze(clazz.name, method);
	}

	@Test void incompatibleNativeTargetsFailExplicitly() {
		var method = new MethodNode(Opcodes.ACC_PRIVATE, "changed", "()V", null, null);
		method.instructions.add(new InsnNode(Opcodes.RETURN));
		assertThrows(IllegalStateException.class, () -> new HideHeadUnderTrophyTransformer().transform(method, null));
	}
}
