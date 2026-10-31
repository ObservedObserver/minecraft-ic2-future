package diagnostics;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import org.objectweb.asm.*;
/** Passive test-only logging. Never changes a screen, overlay, key, input or method result. */
public final class NormalStartupObserver {
  public static void premain(String unused, Instrumentation instrumentation) {
    instrumentation.addTransformer(new ClassFileTransformer() {
      @Override public byte[] transform(ClassLoader loader,String name,Class<?> redef,ProtectionDomain domain,byte[] bytes) {
        if (!name.equals("net/minecraft/client/gui/Gui") && !name.equals("net/minecraft/client/gui/screens/TitleScreen") && !name.equals("ic2/core/util/KeyboardClient")) return null;
        ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
        reader.accept(new ClassVisitor(Opcodes.ASM9,writer) {
          @Override public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions) {
            MethodVisitor base=super.visitMethod(access,method,desc,signature,exceptions);
            return new MethodVisitor(Opcodes.ASM9,base) {
              void print(String message) {
                super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");super.visitLdcInsn(message);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V",false);
              }
              @Override public void visitCode() {
                super.visitCode();
                if (name.equals("net/minecraft/client/gui/Gui") && (method.equals("setScreen")||method.equals("setOverlay"))) {
                  super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");super.visitLdcInsn("IC2R_NORMAL_"+method+"=");
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","print","(Ljava/lang/String;)V",false);
                  super.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");super.visitVarInsn(Opcodes.ALOAD,1);
                  super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/Object;)V",false);
                }
              }
              @Override public void visitInsn(int opcode) {
                if (opcode==Opcodes.RETURN && name.equals("net/minecraft/client/gui/screens/TitleScreen") && method.equals("extractRenderState")) print("IC2R_NORMAL_TITLE_EXTRACTION_RETURNED");
                if (opcode==Opcodes.RETURN && name.equals("ic2/core/util/KeyboardClient") && method.equals("sendKeyUpdate")) print("IC2R_NORMAL_KEYBOARD_RETURNED");
                super.visitInsn(opcode);
              }
            };
          }
        },0);return writer.toByteArray();
      }
    });
  }
}
