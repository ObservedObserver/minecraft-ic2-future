#!/usr/bin/env python3
"""Package the exported official ModDev client launch as a local macOS test application."""
import json, shlex, shutil, subprocess
from pathlib import Path
root=Path(__file__).resolve().parents[1]
j=json.loads((root/'evidence/client-launch.json').read_text())
jdk=root/'.toolchains/jdk-25.0.5+8/Contents/Home'
staging=root/'build/mac-test-input';staging.mkdir(exist_ok=True)
main=next(Path(p) for p in j['classpath'] if '/DevLaunch/' in p)
shutil.copy2(main,staging/main.name)
out=root/'build/mac-test-app'
subprocess.run([str(jdk/'bin/jpackage'),'--type','app-image','--name','IC2RVerification','--app-version','1.0','--mac-package-identifier','local.ic2r.verification','--input',str(staging),'--main-jar',main.name,'--main-class',j['mainClass'],'--runtime-image',str(jdk),'--dest',str(out)],check=True)
def expand(args):
 result=[]
 for arg in args:
  if arg.startswith('@'):result.extend(shlex.split(Path(arg[1:]).read_text(),comments=True))
  else:result.append(arg)
 return result
args=expand(j['jvmArgs']); normalized=[];i=0
while i<len(args):
 arg=args[i]
 if arg in ('--add-opens','--add-exports','--enable-native-access'):
  normalized.append(arg+'='+args[i+1]);i+=2
 else:normalized.append(arg);i+=1
program=expand(j['args'])
config=out/'IC2RVerification.app/Contents/app/IC2RVerification.cfg'
config.write_text('[Application]\napp.classpath='+':'.join(j['classpath'])+'\napp.mainclass='+j['mainClass']+'\n\n[JavaOptions]\n'+''.join('java-options='+a+'\n' for a in dict.fromkeys(normalized))+'\n[ArgOptions]\n'+''.join('arguments='+a+'\n' for a in program))
print(out/'IC2RVerification.app')
