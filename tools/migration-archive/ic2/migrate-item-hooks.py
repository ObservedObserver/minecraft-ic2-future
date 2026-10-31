from pathlib import Path
import re
files=list(Path('src/main/java/ic2').rglob('*.java'))
classes={}
for p in files:
 s=p.read_text();m=re.search(r'\bclass\s+(\w+)([^\{]*)\{',s)
 if m:classes[m[1]]=(p,re.search(r'\bextends\s+(\w+)',m[2]),s)
oldtooltips={name for name,(p,parent,s) in classes.items() if re.search(r'public void appendHoverText\([^\{]*List<Component>',s)}
oldticks={name for name,(p,parent,s) in classes.items() if re.search(r'public void inventoryTick\([^\{]*boolean\s+\w+\)\s*\{',s)}
def ancestor_has(name,targets):
 seen=set()
 while name in classes and name not in seen:
  seen.add(name);parent=classes[name][1]
  if not parent:return False
  name=parent[1]
  if name in targets:return True
 return False
for name,(p,parent,orig) in classes.items():
 s=orig
 if name in oldtooltips:
  pattern=r'(?:  @Override\s*\n)?  public void appendHoverText\(([^\{]+)\)\s*\{'
  m=re.search(pattern,s)
  params=m[1].split(',');names=[re.findall(r'\w+',t)[-1] for t in params]
  if len(names)!=4:raise Exception((name,names))
  stack,context,tooltip,flag=names
  bridge='''  @Override
  public void appendHoverText(net.minecraft.world.item.ItemStack stack,
      net.minecraft.world.item.Item.TooltipContext context,
      net.minecraft.world.item.component.TooltipDisplay display,
      java.util.function.Consumer<net.minecraft.network.chat.Component> output,
      net.minecraft.world.item.TooltipFlag flag) {
    if (display.hideTooltip()) return;
    java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
    this.appendHoverText(stack, context, lines, flag);
    lines.forEach(output);
  }

'''
  signature='  public void appendHoverText('+m[1]+') {'
  s=s[:m.start()]+bridge+signature+s[m.end():]
  if not ancestor_has(name,oldtooltips):
   s=re.sub(r'super\.appendHoverText\((\w+),\s*(\w+),\s*(\w+),\s*(\w+)\)',r'super.appendHoverText(\1, \2, net.minecraft.world.item.component.TooltipDisplay.DEFAULT, \3::add, \4)',s)
 if name in oldticks:
  m=re.search(r'(?:  @Override\s*\n)?  public void inventoryTick\(([^\{]+)\)\s*\{',s)
  bridge='''  @Override
  public void inventoryTick(net.minecraft.world.item.ItemStack stack,
      net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity owner,
      net.minecraft.world.entity.EquipmentSlot slot) {
    this.inventoryTick(stack, level, owner, ic2.compat.InventoryCompat.indexOf(owner, stack, slot),
        slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND);
  }

'''
  s=s[:m.start()]+bridge+'  public void inventoryTick('+m[1]+') {'+s[m.end():]
  header=re.search(r'\bclass\s+'+name+r'([^\{]*)\{',s)
  suffix=header[1]
  suffix+=(', ' if 'implements ' in suffix else ' implements ')+'ic2.compat.LegacyInventoryTicker '
  s=s[:header.start(1)]+suffix+s[header.end(1):]
  if not ancestor_has(name,oldticks):
   s=re.sub(r'super\.inventoryTick\((\w+),\s*(\w+),\s*(\w+),\s*(\w+),\s*(\w+)\)',r'super.inventoryTick(\1, (net.minecraft.server.level.ServerLevel) \2, \3, \5 ? net.minecraft.world.entity.EquipmentSlot.MAINHAND : null)',s)
 if s!=orig:p.write_text(s)
print('Bridged',len(oldtooltips),'tooltip classes and',len(oldticks),'inventory ticker classes')
