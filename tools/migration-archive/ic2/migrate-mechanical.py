from pathlib import Path
import json,re
moves=json.loads(Path('evidence/moved-import-candidates.json').read_text())
moves={old:new[0] for old,new in moves.items() if len(new)==1}
renames={
 'net.minecraft.world.inventory.ClickType':'net.minecraft.world.inventory.ContainerInput',
 'net.minecraft.world.level.block.FarmBlock':'net.minecraft.world.level.block.FarmlandBlock',
 'net.minecraft.client.gui.GuiGraphics':'net.minecraft.client.gui.GuiGraphicsExtractor',
}
# Only exact package moves whose target exists in the official source archive are applied here.
import zipfile
z=zipfile.ZipFile('build/moddev/artifacts/minecraft-patched-26.3.0.83-sources.jar')
paths=set(z.namelist())
for old,new in renames.items():
 if new.replace('.','/')+'.java' in paths:moves[old]=new
counts={}
for p in Path('src').rglob('*.java'):
 s=p.read_text();old=s
 for before,after in moves.items():
  if before in s:
   s=s.replace(before,after)
   if before.rsplit('.',1)[-1]!=after.rsplit('.',1)[-1]:
    s=re.sub(r'\b'+before.rsplit('.',1)[-1]+r'\b',after.rsplit('.',1)[-1],s)
 s=re.sub(r'\.isClientSide\b(?!\s*\()', '.isClientSide()',s)
 s=s.replace('.getAllKeys()', '.keySet()')
 if s!=old:p.write_text(s);counts[str(p)]=1
print('Updated',len(counts),'sources; exact package moves:',len(moves))
