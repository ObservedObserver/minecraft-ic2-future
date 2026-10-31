package twilightforest.asm.transformers.carver;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import net.neoforged.neoforgespi.transformation.SimpleMethodProcessor;
import net.neoforged.neoforgespi.transformation.SimpleTransformationContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import twilightforest.asm.ASMUtil;

/** Supplies the chunk-local block policy context removed from WorldCarver in 26.3. */
public final class TFCaveGenerationTransformer extends SimpleMethodProcessor {
    private static final String MASK = "net/minecraft/world/level/chunk/CarvingMask";
    private static final String TF_MASK = "twilightforest/world/components/carver/TFCarvingMask";
    private static final String CARVE = "(Lnet/minecraft/world/level/levelgen/WorldGenerationContext;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/CarverOutput;)Z";
    private static final String TARGET = "(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V";
    @Override public ProcessorName name() { return ASMUtil.named("tf_cave_generation_context"); }
    @Override public Set<Target> targets() { return Set.of(new Target("net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator", "generateCarvers", TARGET)); }

    @Override
    public void transform(MethodNode node, SimpleTransformationContext context) {
        int allocations = 0, constructors = 0, calls = 0;
        for (AbstractInsnNode insn : node.instructions.toArray()) {
            if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && type.desc.equals(MASK)) {
                type.desc = TF_MASK; allocations++;
            } else if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                    && call.owner.equals(MASK) && call.name.equals("<init>") && call.desc.equals("(II)V")) {
                call.owner = TF_MASK; constructors++;
            } else if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE
                    && call.owner.equals("net/minecraft/world/level/levelgen/carver/WorldCarver") && call.name.equals("carve") && call.desc.equals(CARVE)) {
                node.instructions.insertBefore(call, ASMUtil.listOf(
                    new VarInsnNode(Opcodes.ALOAD, 1), // ChunkAccess
                    new VarInsnNode(Opcodes.ALOAD, 4), // RandomState
                    new VarInsnNode(Opcodes.ALOAD, 7), // MaterialRule
                    new VarInsnNode(Opcodes.ALOAD, 3), // NoiseChunk
                    new VarInsnNode(Opcodes.ALOAD, 5), // BiomeManager
                    new VarInsnNode(Opcodes.ALOAD, 0)  // Generator
                ));
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = "twilightforest/world/components/carver/TFCaveGenerationHooks";
                call.itf = false;
                call.desc = "(Lnet/minecraft/world/level/levelgen/carver/WorldCarver;" + CARVE.substring(1, CARVE.length() - 2)
                    + "Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;)Z";
                calls++;
            }
        }
        if (allocations != 1 || constructors != 1 || calls != 1) {
            throw new IllegalStateException("TF cave bridge targets changed: " + allocations + "/" + constructors + "/" + calls);
        }
        node.maxStack += 6;
    }
}
