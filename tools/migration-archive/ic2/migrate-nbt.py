from pathlib import Path
import re
fallback={'Byte':'(byte) 0','Short':'(short) 0','Int':'0','Long':'0L','Float':'0.0F','Double':'0.0','Boolean':'false','String':'""'}
changed=calls=0
for p in Path('src').rglob('*.java'):
 s=p.read_text();orig=s
 names=set(re.findall(r'\b(?:CompoundTag|ListTag)\s+(\w+)',s))
 # CompoundTag-valued common expressions are handled by later compiler-guided passes.
 if not names:continue
 pat=re.compile(r'((?:this\.)?(?:'+ '|'.join(map(re.escape,sorted(names,key=len,reverse=True)))+r'))\.(get(?:Byte|Short|Int|Long|Float|Double|Boolean|String|Compound|List|ByteArray|IntArray|LongArray)|contains)\(')
 edits=[]
 for m in pat.finditer(s):
  start=m.end();i=start;depth=1;quote=None;escape=False
  while i<len(s) and depth:
   c=s[i]
   if quote:
    if escape:escape=False
    elif c=='\\':escape=True
    elif c==quote:quote=None
   elif c in ['"',"'"]:quote=c
   elif c=='(':depth+=1
   elif c==')':depth-=1
   i+=1
  arg=s[start:i-1];method=m[2];typ=method[3:]
  if method=='contains':
   if ',' not in arg:continue
   repl='ic2.compat.NbtCompat.contains('+m[1]+', '+arg+')'
  elif typ=='Compound':repl=m[1]+'.getCompoundOrEmpty('+arg+')'
  elif typ=='List':repl='ic2.compat.NbtCompat.getList('+m[1]+', '+arg+')' if ',' in arg else m[1]+'.getListOrEmpty('+arg+')'
  elif typ.endswith('Array'):repl=m[1]+'.'+method+'('+arg+').orElseGet(() -> new '+{'ByteArray':'byte','IntArray':'int','LongArray':'long'}[typ]+'[0])'
  elif typ in fallback:repl=m[1]+'.'+method+'Or('+arg+', '+fallback[typ]+')'
  else:continue
  edits.append((m.start(),i,repl));calls+=1
 for a,b,repl in reversed(edits):s=s[:a]+repl+s[b:]
 if s!=orig:p.write_text(s);changed+=1
print('Updated',calls,'NBT calls in',changed,'sources')
