package twilightforest.asm.transformers.chunk;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import net.neoforged.neoforgespi.transformation.SimpleMethodProcessor;
import net.neoforged.neoforgespi.transformation.SimpleTransformationContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import twilightforest.asm.ASMUtil;

/** Run coverings after buildTerrain's surface pass and before its carver pass. */
public class ChunkStatusTaskTransformer extends SimpleMethodProcessor {
	private static final String TARGET = "(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeManager;Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/levelgen/material/rule/MaterialRule;)V";

	@Override public ProcessorName name() { return ASMUtil.named("chunk_status_task"); }
	@Override public Set<Target> targets() {
		return Set.of(new Target("net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator", "generateCarvers", TARGET));
	}
	@Override public void transform(MethodNode node, SimpleTransformationContext context) {
		if (!node.name.equals("generateCarvers") || !node.desc.equals(TARGET) || node.instructions.size() == 0) {
			throw new IllegalStateException("TF covering generation target changed");
		}
		if (ASMUtil.findMethodInstructions(node, Opcodes.INVOKESTATIC,
				"twilightforest/asmhooks/WorldgenHooks", "chunkBlanketing",
				"(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/server/level/WorldGenRegion;)V").findAny().isPresent()) {
			throw new IllegalStateException("TF coverings already inserted");
		}
		node.instructions.insert(ASMUtil.listOf(
			new VarInsnNode(Opcodes.ALOAD, 1),
			new VarInsnNode(Opcodes.ALOAD, 6),
			new MethodInsnNode(Opcodes.INVOKESTATIC, "twilightforest/asmhooks/WorldgenHooks", "chunkBlanketing",
				"(Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/server/level/WorldGenRegion;)V", false)));
		node.maxStack += 2;
	}
}
