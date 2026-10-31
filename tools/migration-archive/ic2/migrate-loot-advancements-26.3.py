from pathlib import Path
import json,collections
root=Path('src/main/resources/data/ic2');counts=collections.Counter()
def item_predicate(d):
 if 'tag' in d and set(d).issubset({'tag','count','enchantments','predicates','components','nbt'}):
  d['items']='#'+d.pop('tag');counts['item_tags']+=1
 if 'enchantments' in d:
  predicates=d.setdefault('predicates',{});ench=d.pop('enchantments')
  for entry in ench:
   if 'enchantment' in entry:entry['enchantments']=entry.pop('enchantment')
  predicates['minecraft:enchantments']=ench;counts['enchantment_predicates']+=1
 return d
def loot(d):
 if isinstance(d,list):return [loot(x) for x in d]
 if not isinstance(d,dict):return d
 d={k:loot(v) for k,v in d.items()}
 if 'conditions' in d and isinstance(d['conditions'],list):
  conditions=d.pop('conditions')
  if conditions:d['condition']=conditions[0] if len(conditions)==1 else {'type':'minecraft:all_of','terms':conditions}
  counts['condition_lists']+=1
 if 'functions' in d:
  assert isinstance(d['functions'],list)
  d['modifier']=d.pop('functions');counts['modifier_lists']+=1
 if isinstance(d.get('condition'),str):d['type']=d.pop('condition');counts['condition_types']+=1
 if 'function' in d:d['type']=d.pop('function');counts['modifier_types']+=1
 if d.get('type')=='minecraft:match_tool' and 'predicate' in d:d['predicate']=item_predicate(d['predicate'])
 return d
for p in (root/'loot_table').rglob('*.json'):
 d=json.loads(p.read_text());new=loot(d)
 if new!=d:p.write_text(json.dumps(new,indent=2)+'\n')
for p in (root/'advancement').rglob('*.json'):
 d=json.loads(p.read_text());before=json.dumps(d)
 for criterion in d.get('criteria',{}).values():
  conditions=criterion.get('conditions',{})
  if criterion.get('trigger')=='minecraft:inventory_changed':
   for item in conditions.get('items',[]):item_predicate(item)
  if criterion.get('trigger')=='minecraft:player_killed_entity':
   pred=conditions.get('entity',{})
   if 'type' in pred:
    conditions['entity']={'type':'minecraft:entity_properties','entity':'this','predicate':{'minecraft:entity_type':pred['type']}}
   source=conditions.get('killing_blow',{}).get('source_entity',{})
   if 'equipment' in source:source['minecraft:equipment']=source.pop('equipment')
 if before!=json.dumps(d):p.write_text(json.dumps(d,indent=2)+'\n')
p=root/'recipe/macerator/chain_to_iron_nugget.json';p.write_text(p.read_text().replace('minecraft:chain','minecraft:iron_chain'))
p=root/'advancement/recipes/shaped/heat_exchanger_from_gold.json';s=p.read_text().replace('heat_exchanger_from_gold','heat_exchanger_from_copper').replace('plates/gold','plates/copper');p.write_text(s)
Path('evidence/phase-5-loot-migration.json').write_text(json.dumps(counts,indent=2)+'\n');print(dict(counts))
