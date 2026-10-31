package twilightforest.asm.transformers.block;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import net.neoforged.neoforgespi.transformation.SimpleMethodProcessor;
import net.neoforged.neoforgespi.transformation.SimpleTransformationContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import twilightforest.asm.ASMUtil;

/** 26.3 computes block restitution in Entity, rather than SlimeBlock.bounceUp. */
public final class SlimeBlockBounceUpTransformer extends SimpleMethodProcessor {
	@Override public ProcessorName name() { return ASMUtil.named("slime_block_bounce_up"); }
	@Override public Set<Target> targets() {
		return Set.of(new Target("net.minecraft.world.entity.Entity", "restituteMovementAfterCollisions",
			"(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;ZZLnet/minecraft/world/phys/Vec3;)V"));
	}
	@Override public void transform(MethodNode node, SimpleTransformationContext context) {
		var calls = ASMUtil.findMethodInstructions(node, Opcodes.INVOKEVIRTUAL,
			"net/minecraft/world/entity/Entity", "getBlockBounciness",
			"(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)D").toList();
		if (calls.size() != 1) throw new IllegalStateException("TF Entity.restituteMovementAfterCollisions: expected one getBlockBounciness call, found " + calls.size());
		node.instructions.insert(calls.getFirst(), ASMUtil.listOf(
			new VarInsnNode(Opcodes.ALOAD, 0),
			new VarInsnNode(Opcodes.ALOAD, 2),
			new MethodInsnNode(Opcodes.INVOKESTATIC, "twilightforest/asmhooks/BlockHooks", "filterSlimeRestitution",
				"(DLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;)D", false)));
		node.maxStack += 2;
	}
}
