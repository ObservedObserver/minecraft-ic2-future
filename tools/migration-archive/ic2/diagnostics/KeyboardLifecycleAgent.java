package diagnostics;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;

/** Test-only: observes early singleton capture and selects the gameplay keyboard branch.
 * Does not change the real GUI, input events, keys, network handlers, or the on-disk mod JAR.
 */
public final class KeyboardLifecycleAgent {
  public static void premain(String mode, Instrumentation instrumentation) {
    boolean gameplayBranch = "keyboard-branch".equals(mode);
    instrumentation.addTransformer(new ClassFileTransformer() {
      @Override public byte[] transform(ClassLoader loader, String name, Class<?> redef,
          ProtectionDomain domain, byte[] bytes) {
        if (!name.equals("ic2/core/util/KeyboardClient") && !name.equals("ic2/core/GuiOverlayer")) return null;
        System.out.println("IC2R_DIAGNOSTIC_TRANSFORM=" + name + ";forceKeyboardBranch=" + gameplayBranch);
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
          @Override public MethodVisitor visitMethod(int access, String method, String desc,
              String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, method, desc, signature, exceptions)) {
              @Override public void visitMethodInsn(int opcode, String owner, String called, String descriptor, boolean itf) {
                if (gameplayBranch && name.equals("ic2/core/util/KeyboardClient")
                    && method.equals("sendKeyUpdate") && owner.equals("net/minecraft/client/gui/Gui")
                    && called.equals("screen")) {
                  // Substitute only the method's local screen result to exercise its in-game branch.
                  // The actual Minecraft GUI and native window are never changed.
                  super.visitInsn(Opcodes.POP);
                  super.visitInsn(Opcodes.ACONST_NULL);
                } else super.visitMethodInsn(opcode, owner, called, descriptor, itf);
              }
              @Override public void visitFieldInsn(int opcode, String owner, String field, String descriptor) {
                super.visitFieldInsn(opcode, owner, field, descriptor);
                if (opcode == Opcodes.GETFIELD && owner.equals("net/minecraft/client/Options")
                    && (field.equals("keyUp") || field.equals("keyJump"))) {
                  super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                  super.visitLdcInsn("IC2R_DIAGNOSTIC_OPTION_READ=" + field);
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                }
              }
              @Override public void visitInsn(int opcode) {
                if (opcode == Opcodes.RETURN && method.equals("<init>")) {
                  super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                  super.visitLdcInsn("IC2R_DIAGNOSTIC_CONSTRUCTOR_SINGLETON[" + name + "]=");
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);
                  super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                  super.visitMethodInsn(Opcodes.INVOKESTATIC, "net/minecraft/client/Minecraft", "getInstance", "()Lnet/minecraft/client/Minecraft;", false);
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
                }
                if (opcode == Opcodes.RETURN && gameplayBranch && name.equals("ic2/core/util/KeyboardClient") && method.equals("sendKeyUpdate")) {
                  super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                  super.visitLdcInsn("IC2R_DIAGNOSTIC_GAMEPLAY_KEY_POLL_RETURNED");
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                }
                super.visitInsn(opcode);
              }
            };
          }
        }, 0);
        return writer.toByteArray();
      }
    });
  }
}
