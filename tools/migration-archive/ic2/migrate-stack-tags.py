from pathlib import Path
import re,json
# This one-time migration changes only put/remove statements on known ItemStack tag copies.
def mask(s):
 return re.sub(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',lambda m:' '*len(m[0]),s,flags=re.S)
def endpair(s,start,op='(',cl=')'):
 depth=1;i=start+1
 while depth:
  if s[i]==op:depth+=1
  elif s[i]==cl:depth-=1
  i+=1
 return i
def args(s):
 ms=mask(s);depth=0
 for i,c in enumerate(ms):
  if c in '([{':depth+=1
  elif c in ')]}':depth-=1
  elif c==',' and depth==0:return s[:i].strip(),s[i+1:].strip()
 raise ValueError(s)
def val(method,v):
 if method=='put':return v
 t=method[3:]
 if t in ['IntArray','ByteArray','LongArray']:return 'new net.minecraft.nbt.'+t+'Tag('+v+')'
 if t=='Boolean':t='Byte'
 return 'net.minecraft.nbt.'+t+'Tag.valueOf('+v+')'
changes=[]
for p in Path('src/main/java').rglob('*.java'):
 s=p.read_text();ms=mask(s);edits=[]
 # Known local roots, restricted to their lexical declaration block.
 for m in re.finditer(r'CompoundTag (\w+) = StackUtil.getOrCreateNbtData\(',s):
  oe=m.end()-1;ce=endpair(ms,oe);stack=s[oe+1:ce-1]
  opens=[]
  for i,c in enumerate(ms[:m.start()]):
   if c=='{':opens.append(i)
   elif c=='}':opens.pop()
  stop=endpair(ms,opens[-1],'{','}')
  for mut in re.finditer(r'\b'+m[1]+r'\.(put\w*|remove)\(',s[ce:stop]):
   a=ce+mut.start();b=ce+mut.end()-1;e=endpair(ms,b)
   method=mut[1];arg=s[b+1:e-1]
   if method=='remove':replacement=f'StackUtil.removeTagValue({stack}, {m[1]}, {arg})'
   else:
    key,value=args(arg);replacement=f'StackUtil.setTagValue({stack}, {m[1]}, {key}, {val(method,value)})'
   edits.append((a,e,replacement))
 # Direct calls need no local root.
 for m in re.finditer(r'StackUtil.getOrCreateNbtData\(',s):
  ce=endpair(ms,m.end()-1);mut=re.match(r'\s*\.(put\w*|remove)\(',s[ce:])
  if not mut:continue
  b=ce+mut.end()-1;e=endpair(ms,b);stack=s[m.end():ce-1];method=mut[1];arg=s[b+1:e-1]
  if method=='remove':replacement=f'StackUtil.removeTagValue({stack}, {arg})'
  else:
   key,value=args(arg);replacement=f'StackUtil.setTagValue({stack}, {key}, {val(method,value)})'
  edits.append((m.start(),e,replacement))
 for a,b,r in sorted(set(edits),reverse=True):s=s[:a]+r+s[b:]
 if edits:p.write_text(s);changes.append({'file':str(p),'statements':len(edits)})
Path('evidence/stack-tag-migration.json').write_text(json.dumps(changes,indent=2)+'\n')
print('migrated',sum(c['statements'] for c in changes),'mutations in',len(changes),'files')
