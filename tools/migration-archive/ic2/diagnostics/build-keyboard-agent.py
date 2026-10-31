#!/usr/bin/env python3
"""Build a test-only JVM agent from source using this project's official cached ASM/JDK."""
from pathlib import Path
import subprocess,zipfile
root=Path(__file__).resolve().parents[2]
jdk=root/'.toolchains/jdk-25.0.5+8/Contents/Home/bin'
asm=next((root/'.gradle-user-home/caches/modules-2/files-2.1/org.ow2.asm/asm/9.11').rglob('asm-9.11.jar'))
out=root/'build/client-diagnostics';out.mkdir(exist_ok=True)
subprocess.run([str(jdk/'javac'),'-cp',str(asm),'-d',str(out),str(root/'scripts/diagnostics/KeyboardLifecycleAgent.java')],check=True)
jar=out/'keyboard-lifecycle-agent.jar'
with zipfile.ZipFile(jar,'w') as z:
 z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: diagnostics.KeyboardLifecycleAgent\n\n')
 for p in (out/'diagnostics').rglob('*.class'):z.write(p,str(p.relative_to(out)))
 with zipfile.ZipFile(asm) as az:
  for n in az.namelist():
   if n.startswith('org/objectweb/asm/') and n.endswith('.class'):z.writestr(n,az.read(n))
print(jar)
