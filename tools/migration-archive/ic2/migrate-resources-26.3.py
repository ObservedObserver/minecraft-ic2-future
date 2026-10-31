from pathlib import Path
import json,re,shutil
r=Path('src/main/resources');a=r/'assets/ic2'
def write(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
metadata=json.loads((r/'pack.mcmeta').read_text());metadata['pack']={'min_format':[97,1],'max_format':[121,0],'description':'IC2R Resource Pack'};write(r/'pack.mcmeta',metadata)
names=re.findall(r'register\(\s*"([^"]+)"',Path('src/main/java/ic2/core/ref/Ic2Items.java').read_text())
def leaf(model,name):
 d={'type':'minecraft:model','model':model}
 if name in ['rubber_leaves','empty_cell']:d['tints']=[{'type':'ic2:legacy_tint','index':i} for i in range(2 if name=='empty_cell' else 1)]
 return d
for name in names:
 old=json.loads((a/f'models/item/{name}.json').read_text());model=leaf('ic2:item/'+name,name)
 if old.get('loader')=='ic2:mask_overlay':
  model={k:v for k,v in old.items() if k!='loader'};model['type']='ic2:mask_overlay'
  write(a/f'models/item/{name}.json',{'parent':old['base']})
 for override in old.get('overrides',[]):
  assert len(override['predicate'])==1,override
  prop,threshold=next(iter(override['predicate'].items()))
  model={'type':'minecraft:range_dispatch','property':'ic2:legacy','property_id':prop,
   'fallback':model,'entries':[{'threshold':threshold,'model':leaf(override['model'],name)}]}
 write(a/f'items/{name}.json',{'model':model})
# Real native fluid bucket geometry, base/cover/mask from the same vanilla assets.
fluids=re.findall(r'create\(\s*"([^"]+)"',Path('src/main/java/ic2/core/ref/Ic2Fluids.java').read_text())
for fluid in fluids:
 write(a/f'items/{fluid}_bucket.json',{'model':{'type':'neoforge:fluid_container','fluid':'ic2:'+fluid,
  'textures':{'base':'minecraft:item/bucket','fluid':'neoforge:item/mask/bucket_fluid','cover':'neoforge:item/mask/bucket_fluid_cover'}}})
# Preserve dynamic machine retexturing, with facing/active selected by the native model snapshot.
count=0
for p in (a/'models/block/be').rglob('*.json'):
 d=json.loads(p.read_text());name=p.stem;bs=a/f'blockstates/{name}.json'
 if not bs.exists():continue
 if d.get('loader')=='ic2:wall':write(bs,{'variants':{'':{'type':'ic2:wall'}}});continue
 if d.get('loader')!='ic2:be':continue
 state=json.loads(bs.read_text());assert 'variants' in state,bs
 id='ic2:'+str(p.relative_to(a/'models').with_suffix(''))
 for variant in state['variants']:
  state['variants'][variant]={'type':'ic2:be','id':id}
 write(bs,state);count+=1
# Both old procedural cable directories and modern block textures must be stitched.
write(r/'assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:directory','source':'blocks','prefix':'blocks/'}]})
# Port armor assets to the native equipment texture directory without altering original PNGs.
materials=re.findall(r'"ic2:(ic2_[a-z_]+)"',Path('src/main/java/ic2/core/ref/Ic2ArmorMaterials.java').read_text())
for name in dict.fromkeys(materials):
 layers={}
 for kind,number in [('humanoid',1),('humanoid_baby',1),('humanoid_leggings',2)]:
  source=a/f'textures/models/armor/{name}_layer_{number}.png'
  if not source.exists():continue
  target=a/f'textures/entity/equipment/{kind}/{name}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
  layer={'texture':'ic2:'+name}
  if name=='ic2_quantum':layer['dyeable']={'color_when_undyed':0xFFFFFF}
  layers[kind]=[layer]
  overlay=a/f'textures/models/armor/{name}_layer_{number}_overlay.png'
  if overlay.exists():
   shutil.copyfile(overlay,target.with_stem(name+'_overlay'));layers[kind].append({'texture':'ic2:'+name+'_overlay'})
 assert layers,name
 write(a/f'equipment/{name}.json',{'layers':layers})
write(Path('evidence/phase-5-resource-migration.json'),{'item_definitions':len(names),'fluid_buckets':len(fluids),'dynamic_machine_blockstates':count,'equipment_assets':len(set(materials))})
print(len(names),'item definitions;',count,'dynamic machine blockstates')
