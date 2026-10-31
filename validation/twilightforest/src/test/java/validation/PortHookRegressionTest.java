package validation;

import com.google.gson.JsonParser;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import twilightforest.asm.transformers.armor.ArmorVisibilityRenderingTransformer;
import twilightforest.asm.transformers.block.SlimeBlockBounceUpTransformer;
import twilightforest.asm.transformers.chunk.ChunkStatusTaskTransformer;
import net.neoforged.neoforgespi.transformation.SimpleMethodProcessor;
import static org.junit.jupiter.api.Assertions.*;

class PortHookRegressionTest {
    private static Path projectRoot() {
        return Path.of(Objects.requireNonNull(System.getProperty("twilightforest.projectRoot"),
            "Set -Dtwilightforest.projectRoot to the TF source repository"));
    }
    private static ClassNode read(Class<?> type) throws Exception {
        var node = new ClassNode();
        try (var input = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertNotNull(input); new ClassReader(input).accept(node, 0);
        }
        return node;
    }
    private static MethodNode transform(Class<?> type, SimpleMethodProcessor processor, String hook) throws Exception {
        var node = read(type);
        var target = processor.targets().iterator().next();
        var method = node.methods.stream().filter(m -> m.name.equals(target.methodName())
            && m.desc.equals(target.methodDescriptor())).findFirst().orElseThrow();
        processor.transform(method, null);
        long hooks = Arrays.stream(method.instructions.toArray()).filter(i -> i instanceof MethodInsnNode c
            && c.owner.startsWith("twilightforest/asmhooks/") && c.name.equals(hook)).count();
        assertEquals(1, hooks);
        for (var insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.startsWith("twilightforest/asmhooks/")) {
                var classFile = projectRoot().resolve("build/classes/java/main/" + call.owner + ".class");
                var hookClass = new ClassNode();
                new ClassReader(Files.readAllBytes(classFile)).accept(hookClass, 0);
                assertTrue(hookClass.methods.stream().anyMatch(m -> m.name.equals(call.name)
                    && m.desc.equals(call.desc) && (m.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC))
                    == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)), "Missing public static hook: " + call.owner + "." + call.name + call.desc);
            }
        }
        new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
        return method;
    }
    @Test void coveringsRunOnceAtStartOfCarversAfterSurface() throws Exception {
        var method = transform(NoiseBasedChunkGenerator.class, new ChunkStatusTaskTransformer(), "chunkBlanketing");
        var real = Arrays.stream(method.instructions.toArray()).filter(i -> i.getOpcode() >= 0).toList();
        assertEquals(1, ((VarInsnNode)real.get(0)).var);
        assertEquals(6, ((VarInsnNode)real.get(1)).var);
        assertEquals("chunkBlanketing", ((MethodInsnNode)real.get(2)).name);
        var generator = read(NoiseBasedChunkGenerator.class);
        var callers = generator.methods.stream().filter(m -> Arrays.stream(m.instructions.toArray())
            .anyMatch(i -> i instanceof MethodInsnNode c && c.name.equals("generateCarvers"))).toList();
        assertEquals(1, callers.size());
        var calls = Arrays.stream(callers.getFirst().instructions.toArray()).filter(i -> i instanceof MethodInsnNode)
            .map(i -> ((MethodInsnNode)i).name).toList();
        assertTrue(calls.indexOf("buildSurface") < calls.indexOf("generateCarvers"));
        assertTrue(calls.indexOf("buildSurface") >= 0);
    }
    @Test void visibilityAndBounceUseReal263Targets() throws Exception {
        transform(LivingEntity.class, new ArmorVisibilityRenderingTransformer(), "modifyArmorVisibility");
        var bounce = transform(Entity.class, new SlimeBlockBounceUpTransformer(), "filterSlimeRestitution");
        var instructions = Arrays.stream(bounce.instructions.toArray()).filter(i -> i.getOpcode() >= 0).toList();
        int callIndex = -1;
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i) instanceof MethodInsnNode c && c.name.equals("getBlockBounciness")) callIndex = i;
        }
        assertTrue(callIndex >= 3);
        for (int slot = 0; slot <= 2; slot++) {
            var load = (VarInsnNode)instructions.get(callIndex - 3 + slot);
            assertEquals(Opcodes.ALOAD, load.getOpcode());
            assertEquals(slot, load.var, "The block restitution call must consume this, effectPos and effectState");
        }
        assertTrue(instructions.subList(0, callIndex).stream().noneMatch(i -> i instanceof MethodInsnNode c
            && c.name.equals("setDeltaMovement")), "Velocity must not change before the residual-speed filter");
    }
    @Test void incompatibleTargetsFailInsteadOfSilentlySkipping() {
        var empty = new MethodNode(Opcodes.ACC_PUBLIC, "changed", "()V", null, null);
        empty.instructions.add(new InsnNode(Opcodes.RETURN));
        assertThrows(IllegalStateException.class, () -> new ChunkStatusTaskTransformer().transform(empty, null));
        assertThrows(IllegalStateException.class, () -> new ArmorVisibilityRenderingTransformer().transform(empty, null));
        assertThrows(IllegalStateException.class, () -> new SlimeBlockBounceUpTransformer().transform(empty, null));
    }
    @Test void bothShippedRoutersIncludeStructureDensityWithoutChangingSurfaceEstimate() throws Exception {
        var root = projectRoot();
        for (String name : List.of("twilight_noise_gen", "skylight_noise_gen")) {
            var path = root.resolve("src/generated/resources/data/twilightforest/worldgen/noise_settings/" + name + ".json");
            var router = JsonParser.parseString(Files.readString(path)).getAsJsonObject().getAsJsonObject("noise_router");
            var finalDensity = router.getAsJsonObject("final_density");
            assertEquals("minecraft:add", finalDensity.get("type").getAsString());
            assertEquals(router.get("chunk_surface_level"), finalDensity.get("left"));
            assertEquals("minecraft:beardifier", finalDensity.getAsJsonObject("right").get("type").getAsString());
            // Exercise the actual 26.3 add codec, independently of the hand-written assertions.
            var inline = finalDensity.deepCopy();
            inline.addProperty("left", 0.0);
            var decoded = net.minecraft.world.level.levelgen.densityfunction.DensityFunctions.DIRECT_CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, inline).getOrThrow();
            assertNotNull(decoded);
            var generated = root.resolve("build/generated-port-resources/data/twilightforest/worldgen/noise_settings/" + name + ".json");
            assertArrayEquals(Files.readAllBytes(generated), Files.readAllBytes(path), "Shipped settings must match runClientData");
        }
    }
}
