from pathlib import Path
import re
log=Path('evidence/compile-03.log').read_text().split('> Task :compileJava FAILED')[0]
# Optional-valued getter diagnostics include enough type information to select primitive defaults.
fallback={'Byte':'(byte) 0','Short':'(short) 0','Int':'0','Long':'0L','Float':'0.0F','Double':'0.0','Boolean':'false','String':'""'}
lines=log.splitlines();fixes={}
for i,l in enumerate(lines):
 m=re.match(r'(/[^\n]+\.java):(\d+): error:',l)
 if not m:continue
 p=Path(m[1]);ctx='\n'.join(lines[i+1:i+6])
 if 'Optional<' not in ctx and 'Optional<' not in l:continue
 # Match exact source text from diagnostic, because preceding migrations may have shifted line numbers.
 snippet=lines[i+1].strip()
 for typ,default in fallback.items():
  if typ=='String' and 'Optional<String>' not in ctx and 'Optional<String>' not in l:continue
  method='get'+typ
  pattern=re.compile(r'\.'+method+r'\(([^\n()]*(?:\([^\n()]*\)[^\n()]*)*)\)')
  if not pattern.search(snippet):continue
  content=fixes.setdefault(p,p.read_text())
  if snippet in content:
   replacement=pattern.sub(lambda g:'.'+method+'Or('+g[1]+', '+default+')',snippet)
   fixes[p]=content.replace(snippet,replacement)
for p,s in fixes.items():
 if s!=p.read_text():p.write_text(s)
print('Updated optional NBT reads in',len(fixes),'files')
