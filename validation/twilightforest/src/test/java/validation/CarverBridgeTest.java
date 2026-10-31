package validation;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import twilightforest.asm.transformers.carver.TFCaveGenerationTransformer;
import twilightforest.world.components.carver.TFCarvingMask;
import static org.junit.jupiter.api.Assertions.*;

class CarverBridgeTest {
    @Test
    void immediateTfClaimsAreNotAppliedAgainByTheDeferredNativePass() {
        var mask = new TFCarvingMask(-63, 248);
        assertTrue(mask.claim(15, -40, 0));
        mask.carve(15, -40, 0);
        assertTrue(mask.isEmpty());
        assertFalse(mask.claim(15, -40, 0));
    }

    @Test
    void nativeClaimsRetainTheirDeferredGeometryAndRejectLaterTfClaims() {
        var mask = new TFCarvingMask(-63, 248);
        mask.carve(0, -63, 15);
        mask.carve(0, -62, 15);
        assertFalse(mask.claim(0, -63, 15));
        List<String> columns = new ArrayList<>();
        mask.visit((x, z, bottom, top) -> columns.add(x + ":" + z + ":" + bottom + ":" + top));
        assertEquals(List.of("0:15:-63:-62"), columns);
    }

    @Test
    void ownershipDoesNotLeakAcrossChunksOrColumns() {
        var first = new TFCarvingMask(-63, 248);
        var second = new TFCarvingMask(-63, 248);
        assertTrue(first.claim(1, 248, 2));
        assertTrue(first.claim(2, 248, 1));
        assertTrue(first.claim(1, -63, 2));
        assertTrue(second.claim(1, 248, 2));
        assertFalse(first.claim(1, 248, 2));
    }

    @Test
    void transformerMatchesActual263GeneratorAndPassesStackAnalysis() throws Exception {
        var clazz = new ClassNode();
        try (var input = NoiseBasedChunkGenerator.class.getResourceAsStream("NoiseBasedChunkGenerator.class")) {
            assertNotNull(input);
            new ClassReader(input).accept(clazz, 0);
        }
        var method = clazz.methods.stream().filter(m -> m.name.equals("generateCarvers")).findFirst().orElseThrow();
        new TFCaveGenerationTransformer().transform(method, null);
        long hooks = 0, allocations = 0;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("twilightforest/world/components/carver/TFCaveGenerationHooks")) {
                assertEquals(Opcodes.INVOKESTATIC, call.getOpcode()); hooks++;
            }
            if (insn instanceof TypeInsnNode type && type.desc.equals("twilightforest/world/components/carver/TFCarvingMask")) allocations++;
        }
        assertEquals(1, hooks);
        assertEquals(1, allocations);
        new Analyzer<>(new BasicVerifier()).analyze(clazz.name, method);
    }

    @Test
    void missingTransformerTargetsFailExplicitly() {
        var method = new MethodNode(Opcodes.ACC_PUBLIC, "changedUpstream", "()V", null, null);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        assertThrows(IllegalStateException.class, () -> new TFCaveGenerationTransformer().transform(method, null));
    }
}
