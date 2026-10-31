package diagnostics;
import java.nio.file.Path;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.*;
public final class InspectClientChunk {
  public static void main(String[] args) throws Exception {
    Path file=Path.of(args[0]);
    try (RegionFile region=new RegionFile(new RegionStorageInfo("ic2r-isolated-client-test",null,"chunk"),file,file.getParent(),false);
        var input=region.getChunkDataInputStream(new ChunkPos(0,0))) {
      if(input==null)throw new IllegalStateException("Expected fixture chunk0,0");
      CompoundTag tag=NbtIo.read(input,NbtAccounter.unlimitedHeap());
      ListTag bes=tag.getListOrEmpty("block_entities");
      for (int i=0;i<bes.size();i++) {
        CompoundTag be=bes.getCompoundOrEmpty(i);
        if(be.getIntOr("z",0)==1 && (be.getIntOr("x",0)==1 || be.getIntOr("x",0)==3 || be.getIntOr("x",0)==5)) System.out.println(be);
      }
    }
  }
}
