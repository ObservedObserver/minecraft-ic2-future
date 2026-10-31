from pathlib import Path
import re,json
root=Path('src/main/java')
record=[]
for name,kind in [('Ic2Items','ITEM'),('Ic2Blocks','BLOCK')]:
 p=root/f'ic2/core/ref/{name}.java';s=p.read_text()
 if kind=='BLOCK':
  for field in ['wallSettings','cableSettings','insulatedCableSettings','cropSettings']:
   pattern=r'private static final Properties '+field+r'\s*=\s*([^;]+);'
   s,n=re.subn(pattern,lambda m:'private static Properties '+field+'() {\n    return '+m.group(1)+';\n  }',s)
   assert n==1,(field,n)
   s=re.sub(r'\b'+field+r'\b(?!\s*\()',field+'()',s)
 matches=list(re.finditer(r'\bregister\(\s*"([^\"]+)"\s*,',s))
 for m in reversed(matches):
  i=m.end();j=i;depth=1;quote=False;esc=False
  while depth:
   c=s[j]
   if quote:
    if esc:esc=False
    elif c=='\\':esc=True
    elif c=='"':quote=False
   elif c=='"':quote=True
   elif c=='(':depth+=1
   elif c==')':depth-=1
   j+=1
  body=s[i:j-1];key='net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.'+kind+', IC2.getIdentifier("'+m.group(1)+'"))'
  patterns=[r'new Properties\(\)'] if kind=='ITEM' else [r'Properties\.of\(\)',r'Properties\.ofFullCopy\([^()]+\)',r'\b(?:wallSettings|cableSettings|insulatedCableSettings|cropSettings)\(\)']
  count=0
  for pattern in patterns:
   body,n=re.subn(pattern,lambda x:x.group(0)+'.setId('+key+')',body);count+=n
  record.append({'kind':kind,'name':m.group(1),'property_ids':count})
  s=s[:i]+body+s[j-1:]
 p.write_text(s)
# Dedicated constructors own their one registered ID.
for cls,name,kind in [('ItemCoke','coke','ITEM'),('ItemArmorJetpackElectric','jetpack_electric','ITEM'),('ItemArmorBatpack','batpack','ITEM'),('ItemArmorAdvBatpack','advanced_batpack','ITEM'),('ItemArmorEnergypack','energy_pack','ITEM'),('ItemArmorLappack','lappack','ITEM'),('BlockRefractoryBricks','refractory_bricks','BLOCK')]:
 p=next(root.rglob(cls+'.java'));s=p.read_text();base='new Properties()' if kind=='ITEM' else 'Properties.of()'
 assert base in s
 s=s.replace(base,base+'.setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.'+kind+', ic2.core.IC2.getIdentifier("'+name+'")))');p.write_text(s)
p=root/'ic2/forge/EnvFluidHandlerForge.java';s=p.read_text().replace('BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)','BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(\n                  net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,\n                      Identifier.fromNamespaceAndPath(id.getNamespace(), "fluid_block_" + id.getPath())))').replace('new Properties().craftRemainder','new Properties().setId(net.minecraft.resources.ResourceKey.create(\n                      net.minecraft.core.registries.Registries.ITEM, bucketId)).craftRemainder');p.write_text(s)
Path('evidence/phase-5-registration-ids.json').write_text(json.dumps(record,indent=2)+'\n')
print(len(record),'registrations reviewed')
