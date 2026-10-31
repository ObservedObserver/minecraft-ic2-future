from pathlib import Path
import subprocess,zipfile
root=Path(__file__).resolve().parents[2];jdk=root/'.toolchains/jdk-25.0.5+8/Contents/Home/bin';out=root/'build/client-normal-observer';out.mkdir(exist_ok=True)
asm=next((root/'.gradle-user-home/caches/modules-2/files-2.1/org.ow2.asm/asm/9.11').rglob('asm-9.11.jar'))
subprocess.run([str(jdk/'javac'),'-cp',str(asm),'-d',str(out),str(root/'scripts/diagnostics/NormalStartupObserver.java')],check=True)
with zipfile.ZipFile(out/'normal-startup-observer.jar','w') as z:
 z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: diagnostics.NormalStartupObserver\n\n')
 for p in (out/'diagnostics').rglob('*.class'):z.write(p,str(p.relative_to(out)))
 with zipfile.ZipFile(asm) as a:
  for n in a.namelist():
   if n.startswith('org/objectweb/asm/') and n.endswith('.class'):z.writestr(n,a.read(n))
print(out/'normal-startup-observer.jar')
