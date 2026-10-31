package twilightforest.asm.transformers.armor;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import net.neoforged.neoforgespi.transformation.SimpleMethodProcessor;
import net.neoforged.neoforgespi.transformation.SimpleTransformationContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import twilightforest.asm.ASMUtil;

/** Adjust only armor cover, before vanilla applies its invisible-entity minimum. */
public class ArmorVisibilityRenderingTransformer extends SimpleMethodProcessor {
	@Override public ProcessorName name() { return ASMUtil.named("armor_visibility_rendering"); }
	@Override public Set<Target> targets() {
		return Set.of(new Target("net.minecraft.world.entity.LivingEntity", "getVisibilityPercent",
			"(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)D"));
	}
	@Override public void transform(MethodNode node, SimpleTransformationContext context) {
		var calls = ASMUtil.findMethodInstructions(node, Opcodes.INVOKEVIRTUAL,
			"net/minecraft/world/entity/LivingEntity", "getArmorCoverPercentage", "()F").toList();
		if (calls.size() != 1) throw new IllegalStateException("TF LivingEntity.getVisibilityPercent(ServerLevel, Entity): expected one getArmorCoverPercentage call, found " + calls.size());
		node.instructions.insert(calls.getFirst(), ASMUtil.listOf(
			new VarInsnNode(Opcodes.ALOAD, 0),
			new MethodInsnNode(Opcodes.INVOKESTATIC, "twilightforest/asmhooks/ArmorHooks", "modifyArmorVisibility",
				"(FLnet/minecraft/world/entity/LivingEntity;)F", false)));
		node.maxStack += 1;
	}
}
