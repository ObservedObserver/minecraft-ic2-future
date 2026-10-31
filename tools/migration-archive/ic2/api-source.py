from pathlib import Path
import zipfile,sys
archives=[zipfile.ZipFile(p) for p in ['build/moddev/artifacts/minecraft-patched-26.3.0.83-sources.jar','build/moddev/artifacts/minecraft-patched-26.3.0.83-merged.jar','evidence/neoforge-26.3.0.83-sources.jar']]
term=sys.argv[1]
for z in archives:
 matches=[n for n in z.namelist() if n.endswith('.java') and (n==term.replace('.','/')+'.java' or n.rsplit('/',1)[-1]==term+'.java')]
 if matches:
  for n in matches:
   s=z.read(n).decode();print(n+'\n'+s)
  break
else:
 for z in archives:
  for n in z.namelist():
   if term.lower() in n.lower() and n.endswith('.java'):print(n)
