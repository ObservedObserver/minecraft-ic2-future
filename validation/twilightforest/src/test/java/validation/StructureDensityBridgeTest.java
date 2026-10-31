package validation;

import java.util.List;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.densityfunction.*;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import twilightforest.asm.transformers.beardifier.InjectCustomTerrainBeardifierDuringCreateNoiseChunkTransformer;
import twilightforest.world.components.chunkgenerators.CustomTerrainBeardifier;
import twilightforest.world.components.chunkgenerators.FocusedDensityFunction;
import static org.junit.jupiter.api.Assertions.*;

class StructureDensityBridgeTest {
    @Test
    void structureVolumeIncludesCustomDensityOutsideEmptyVanillaBeardifier() {
        var sphere = new FocusedDensityFunction(0, 0, 0, 4, -4, 0).compileSampler(null);
        var constant = DensityFunctions.constant(1).compileSampler(null);
        var sampler = new CustomTerrainBeardifier(Beardifier.EMPTY, List.of(sphere, constant));
        var volume = new DensityVolume(2, 2, 1, 0, 0, 0, 4, 1, 1);
        var buffer = DensityBuffer.createUnpooled(volume.size());
        sampler.sampleVolume(SamplerContext.EMPTY_UNCACHED, buffer, volume);
        assertEquals(-3, buffer.get(0));
        assertEquals(-2, buffer.get(1));
        assertEquals(1, buffer.get(2));
        assertEquals(1, buffer.get(3));
        for (int x = 0; x < volume.sizeX(); x++) for (int y = 0; y < volume.sizeY(); y++) {
            assertEquals(sampler.sampleValue(SamplerContext.EMPTY_UNCACHED, volume.blockX(x), volume.blockY(y), 0), buffer.get(volume.indexUnchecked(x, y, 0)));
        }
    }

    @Test
    void structureTransformerMatchesActual263CreateNoiseChunkAndPassesStackAnalysis() throws Exception {
        var clazz = new ClassNode();
        try (var input = NoiseBasedChunkGenerator.class.getResourceAsStream("NoiseBasedChunkGenerator.class")) {
            assertNotNull(input); new ClassReader(input).accept(clazz, 0);
        }
        var method = clazz.methods.stream().filter(m -> m.name.equals("createNoiseChunk")).findFirst().orElseThrow();
        new InjectCustomTerrainBeardifierDuringCreateNoiseChunkTransformer().transform(method, null);
        long hooks = 0;
        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals("twilightforest/asmhooks/WorldgenHooks") && call.name.equals("gatherCustomTerrain")) {
                assertEquals(Opcodes.INVOKESTATIC, call.getOpcode());
                assertTrue(call.desc.endsWith("RandomState;)Lnet/minecraft/world/level/levelgen/Beardifier;")); hooks++;
            }
        }
        assertEquals(1, hooks);
        new Analyzer<>(new BasicVerifier()).analyze(clazz.name, method);
    }
}
