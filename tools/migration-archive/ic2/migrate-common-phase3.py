from pathlib import Path
import re
root=Path('src/main/java');log=Path('evidence/compile-14.log').read_text().split('> Task :compileJava FAILED')[0]
# Apply fixes only to the compiler-identified expressions, retaining IC2's legacy overloads.
for match in re.finditer(r'^(/[^\n]+\.java):(\d+): error: ([^\n]+)\n([^\n]+)',log,re.M):
 p=Path(match[1]);s=p.read_text();line=match[4]
 if 'constructor BlockPos' in match[3]:
  old=re.search(r'new BlockPos\((\w+)\)',line)
  if old:s=s.replace(old[0],old[1]+'.immutable()')
 if 'symbol:   method getDescriptionId()' in log[match.start():match.start()+550]:
  # Restrict the known ItemStack diagnostics.
  if p.name in ['WindGameTests.java','ItemCrystalMemory.java','GuiToolScanner.java','ItemDebug.java']:
   newline=line.replace('.getDescriptionId()', '.getItem().getDescriptionId()');s=s.replace(line,newline)
 p.write_text(s)
for p in root.rglob('*.java'):
 s=p.read_text();s=s.replace('FMLEnvironment.dist','FMLEnvironment.getDist()')
 s=s.replace('.setUnsaved(true)', '.markUnsaved()').replace('.setUnsaved(false)', '.tryMarkSaved()')
 s=s.replace('BlockEvent.BreakEvent','net.neoforged.neoforge.event.level.block.BreakBlockEvent')
 if p.name in ['NetworkManager.java','TileEntityTradeOMat.java','LaserBulletEntity.java']:
  s=re.sub(r'\b(player|playerOwner)\.getServer\(\)',r'ic2.compat.WorldCompat.server(\1.level())',s)
 if p.name=='TileEntityTradeOMat.java':s=s.replace('.isOp(player.getGameProfile())','.isOp(new net.minecraft.server.players.NameAndId(player.getGameProfile()))')
 if p.name=='TileEntityPersonalChest.java':s=s.replace('.isOp(profile)','.isOp(new net.minecraft.server.players.NameAndId(profile))')
 if p.name in ['GeneratorGameTests.java','WindGameTests.java','FluidMachineGameTests.java','KineticGeneratorGameTests.java']:
  s=re.sub(r'level\.setWeatherParameters\(', 'ic2.compat.WorldCompat.setWeather(level, ',s)
  s=re.sub(r'level\.setDayTime\(', 'ic2.compat.WorldCompat.setDayTime(level, ',s)
 if p.name in ['ItemElectricTool.java','AbstractItemNanoSaber.java']:
  start=s.index('public boolean hurtEnemy(');brace=s.index('{',start);end=brace+1;depth=1
  while depth:
   if s[end]=='{':depth+=1
   if s[end]=='}':depth-=1
   end+=1
  s=s[:start]+s[start:end].replace('public boolean hurtEnemy(', 'public void hurtEnemy(').replace('return true;', 'return;')+s[end:]
 if p.name in ['ContainerIndustrialWorkbench.java','TileEntityIndustrialWorkbench.java','TileEntityBatchCrafter.java','JetpackGameTests.java','GradualRecipeGameTests.java']:
  s=re.sub(r'(\.assemble\(toCraftingInput\([^)]*\)),\s*(?:this\.base\.getLevel\(\)|this\.getLevel\(\)|world)\.registryAccess\(\)\)',r'\1)',s)
  s=re.sub(r'(\.assemble\(\w+), helper\.getLevel\(\)\.registryAccess\(\)\)',r'\1)',s)
 if p.name=='JetpackGameTests.java':s=s.replace('.getRecipeManager()', '.recipeAccess()')
 if p.name=='QuantumSuitGameTests.java':
  for var,slot in [('helmet','HEAD'),('chest','CHEST'),('leggings','LEGS')]:s=s.replace(f'{var}, helper.getLevel(), player, 0, false',f'{var}, helper.getLevel(), player, EquipmentSlot.{slot}')
 if p.name=='ArmorGameTests.java':s=s.replace('player.hurt(source, 4.0F)','player.hurtServer(helper.getLevel(), source, 4.0F)')
 p.write_text(s)
