from pathlib import Path
import re
log=Path('evidence/compile-05.log').read_text().split('> Task :compileJava FAILED')[0]
blocks=re.split(r'(?=\n/[^\n]+\.java:\d+: error:)', '\n'+log)
for b in blocks:
 m=re.match(r'\n(/[^\n]+\.java):(\d+): error:[^\n]+\n([^\n]+)',b)
 if not m:continue
 p=Path(m[1]);snippet=m[3].strip();replacement=snippet
 if 'method location()' in b and 'ResourceKey<' in b:replacement=replacement.replace('.location()', '.identifier()')
 if 'method getName()' in b and 'GameProfile' in b:replacement=replacement.replace('.getName()', '.name()')
 if 'method getId()' in b and 'GameProfile' in b:replacement=replacement.replace('.getId()', '.id()')
 if 'method toLong()' in b:replacement=replacement.replace('.toLong()', '.pack()')
 if 'method asLong(BlockPos)' in b:replacement=replacement.replace('ChunkPos.asLong(', 'ChunkPos.pack(')
 if 'method getProfiler()' in b:
  replacement=re.sub(r'(?:this\.)?\w+\.getProfiler\(\)', 'net.minecraft.util.profiling.Profiler.get()',replacement)
 if replacement!=snippet:
  s=p.read_text()
  if snippet in s:p.write_text(s.replace(snippet,replacement))
for p in Path('src').rglob('*.java'):
 s=p.read_text();orig=s
 s=s.replace('ItemStack.parseOptional(', 'ic2.compat.NbtCompat.readItem(').replace('FluidStack.parseOptional(', 'ic2.compat.NbtCompat.readFluid(')
 s=s.replace('.getInventory().getSelected()', '.getInventory().getSelectedItem()')
 s=re.sub(r'new ChunkPos\((chunk|chunkPos)\)',r'ChunkPos.unpack(\1)',s)
 s=s.replace('new ChunkPos(this.worldPosition)', 'ChunkPos.containing(this.worldPosition)').replace('new ChunkPos(pos)', 'ChunkPos.containing(pos)')
 s=s.replace('new ChunkPos(helper.absolutePos(LOADER_POS))', 'ChunkPos.containing(helper.absolutePos(LOADER_POS))')
 s=s.replace('new ChunkPos(((ContainerChunkLoader) this.menu).base.getBlockPos())', 'ChunkPos.containing(((ContainerChunkLoader) this.menu).base.getBlockPos())')
 if '.save(' in s and 'ItemStack' in s:
  # All explicit-argument save calls in these sources serialize ItemStack, except the fluid adapter.
  if p.name!='EnvFluidHandlerForge.java':
   s=re.sub(r'((?:this\.)?\w+(?:\[\w+\])?)\.save\(([^\n;]*?)\)',lambda m:'ic2.compat.NbtCompat.saveItem('+m[1]+', '+m[2]+')' if m[2] else m[0],s)
 if s!=orig:p.write_text(s)
p=Path('src/main/java/ic2/forge/EnvFluidHandlerForge.java');s=p.read_text().replace('impl.parent().save(net.minecraft.core.RegistryAccess.EMPTY, new CompoundTag())','ic2.compat.NbtCompat.saveFluid(impl.parent(), net.minecraft.core.RegistryAccess.EMPTY, new CompoundTag())');p.write_text(s)
p=Path('src/main/java/ic2/core/block/personal/PersonalOwnerNbt.java');s=p.read_text().replace('ownerNbt.putUUID("id", owner.id());','ownerNbt.store("id", UUIDUtil.CODEC, owner.id());').replace('tag.hasUUID("id")','tag.read("id", UUIDUtil.CODEC).isPresent()').replace('tag.getUUID("id")','tag.read("id", UUIDUtil.CODEC).orElseThrow()');p.write_text(s)
