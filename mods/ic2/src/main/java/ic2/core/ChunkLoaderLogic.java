package ic2.core;

import ic2.core.block.machine.tileentity.TileEntityChunkLoader;
import ic2.core.event.TickHandler;
import ic2.core.event.WorldData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;

public final class ChunkLoaderLogic {
  private static final net.minecraft.world.level.saveddata.SavedDataType<SavedState>
      savedStateType =
          new net.minecraft.world.level.saveddata.SavedDataType<>(
              IC2.getIdentifier("loaded_chunks"),
              SavedState::new,
              CompoundTag.CODEC.xmap(
                  SavedState::new,
                  state -> state.save(new CompoundTag(), net.minecraft.core.RegistryAccess.EMPTY)));
  public static final TicketType TICKET_TYPE =
      new TicketType(
          0L,
          TicketType.FLAG_LOADING
              | TicketType.FLAG_SIMULATION
              | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

  public static void addChunkLoader(ServerLevel world, BlockPos pos, LongSet chunks) {
    long loaderChunk = ChunkPos.pack(pos);
    if (!chunks.contains(loaderChunk)) {
      throw new IllegalArgumentException("missing own position");
    }

    ChunkLoaderLogic.SavedState state =
        (ChunkLoaderLogic.SavedState) world.getDataStorage().computeIfAbsent(savedStateType);
    ((Set)
            state.chunksToChunkLoaders.computeIfAbsent(
                loaderChunk, ignore -> new ObjectOpenHashSet<>(1)))
        .add(pos);
    state.setDirty();
    WorldData worldData = WorldData.get(world);

    for (long chunk : chunks) {
      Set<BlockPos> loaders =
          (Set<BlockPos>)
              worldData.loadedChunks.computeIfAbsent(chunk, ignore -> new ObjectOpenHashSet<>(1));
      if (loaders.isEmpty()) {
        addChunkTicket(world, ChunkPos.unpack(chunk));
      }

      loaders.add(pos);
    }

    worldData.chunkLoaders.put(pos, new LongOpenHashSet(chunks));
  }

  public static void removeChunkLoader(ServerLevel world, BlockPos pos) {
    ChunkLoaderLogic.SavedState state =
        (ChunkLoaderLogic.SavedState) world.getDataStorage().get(savedStateType);
    if (state != null) {
      long chunkPos = ChunkPos.pack(pos);
      Set<BlockPos> positions = (Set<BlockPos>) state.chunksToChunkLoaders.get(chunkPos);
      if (positions != null && positions.remove(pos)) {
        if (positions.isEmpty()) {
          state.chunksToChunkLoaders.remove(chunkPos);
        }
        state.setDirty();
      }
    }

    WorldData worldData = WorldData.get(world, false);
    if (worldData != null) {
      disableChunkLoader(world, pos, worldData);
    }
  }

  private static void disableChunkLoader(ServerLevel world, BlockPos pos, WorldData worldData) {
    LongSet positions = (LongSet) worldData.chunkLoaders.remove(pos);
    if (positions != null) {

      for (long chunkPos : positions) {
        Set<BlockPos> loaders = (Set<BlockPos>) worldData.loadedChunks.get(chunkPos);
        if (loaders != null && loaders.remove(pos) && loaders.isEmpty()) {
          worldData.loadedChunks.remove(chunkPos);
          removeChunkTicket(world, ChunkPos.unpack(chunkPos));
        }
      }
    }
  }

  public static void updateChunkLoader(ServerLevel world, BlockPos pos, LongSet chunks) {
    long loaderChunk = ChunkPos.pack(pos);
    if (!chunks.contains(loaderChunk)) {
      throw new IllegalArgumentException("missing own position");
    }

    WorldData worldData = WorldData.get(world);
    LongSet prev = (LongSet) worldData.chunkLoaders.get(pos);
    if (prev == null) {
      addChunkLoader(world, pos, chunks);
    } else {
      LongIterator it = prev.longIterator();

      while (it.hasNext()) {
        long chunk = it.nextLong();
        if (!chunks.contains(chunk)) {
          it.remove();
          Set<BlockPos> loaders = (Set<BlockPos>) worldData.loadedChunks.get(chunk);
          loaders.remove(pos);
          if (loaders.isEmpty()) {
            removeChunkTicket(world, ChunkPos.unpack(chunk));
            worldData.loadedChunks.remove(chunk);
          }
        }
      }

      it = chunks.iterator();

      while (it.hasNext()) {
        long chunk = (Long) it.next();
        if (prev.add(chunk)) {
          Set<BlockPos> loaders =
              (Set<BlockPos>)
                  worldData.loadedChunks.computeIfAbsent(
                      chunk, ignore -> new ObjectOpenHashSet<>());
          if (loaders.isEmpty()) {
            addChunkTicket(world, ChunkPos.unpack(chunk));
          }

          loaders.add(pos);
        }
      }
    }
  }

  public static void onWorldLoad(ServerLevel world) {
    ChunkLoaderLogic.SavedState state =
        (ChunkLoaderLogic.SavedState) world.getDataStorage().get(savedStateType);
    if (state != null && !state.chunksToChunkLoaders.isEmpty()) {
      WorldData worldData = WorldData.get(world);

      for (Entry<Set<BlockPos>> entry : state.chunksToChunkLoaders.long2ObjectEntrySet()) {
        long chunkPos = entry.getLongKey();
        Set<BlockPos> loaders = (Set<BlockPos>) entry.getValue();
        worldData.loadedChunks.put(chunkPos, new ObjectOpenHashSet<>(loaders));

        for (BlockPos pos : loaders) {
          ((LongSet) worldData.chunkLoaders.computeIfAbsent(pos, ignore -> new LongOpenHashSet(1)))
              .add(chunkPos);
        }

        addChunkTicket(world, ChunkPos.unpack(chunkPos));
      }
    }
  }

  public static void onChunkLoad(LevelChunk chunk) {
    if (!(chunk.getLevel() instanceof ServerLevel world)) {
      return;
    }

    // Wait until the chunk and its saved block entities have finished loading. Never load
    // another chunk here: the bootstrap ticket must be what brought this one into memory.
    TickHandler.requestSingleWorldTick(
        world,
        ignored -> {
          if (world.getChunkSource().getChunkNow(chunk.getPos().x(), chunk.getPos().z()) != chunk) {
            return;
          }
          SavedState state = world.getDataStorage().get(savedStateType);
          if (state == null) {
            return;
          }
          Set<BlockPos> positions = state.chunksToChunkLoaders.get(chunk.getPos().pack());
          if (positions == null) {
            return;
          }
          for (BlockPos pos : new ObjectOpenHashSet<>(positions)) {
            if (!(chunk.getBlockEntity(pos) instanceof TileEntityChunkLoader loader)
                || !loader.getActive()) {
              removeChunkLoader(world, pos);
            }
          }
        });
  }

  public static void onChunkUnload(LevelChunk chunk) {
    assert !chunk.getLevel().isClientSide();
    ServerLevel world = (ServerLevel) chunk.getLevel();
    ChunkLoaderLogic.SavedState state =
        (ChunkLoaderLogic.SavedState) world.getDataStorage().get(savedStateType);
    if (state != null && !state.chunksToChunkLoaders.isEmpty()) {
      Set<BlockPos> loaders = (Set<BlockPos>) state.chunksToChunkLoaders.get(chunk.getPos().pack());
      if (loaders != null && !loaders.isEmpty()) {
        WorldData worldData = WorldData.get(world, false);
        if (worldData != null) {
          for (BlockPos pos : loaders) {
            disableChunkLoader(world, pos, worldData);
          }
        }
      }
    }
  }

  private static void addChunkTicket(ServerLevel world, ChunkPos pos) {
    world.getChunkSource().addTicketWithRadius(TICKET_TYPE, pos, 2);
  }

  private static void removeChunkTicket(ServerLevel world, ChunkPos pos) {
    world.getChunkSource().removeTicketWithRadius(TICKET_TYPE, pos, 2);
  }

  private static final class SavedState extends SavedData {
    final Long2ObjectMap<Set<BlockPos>> chunksToChunkLoaders = new Long2ObjectOpenHashMap<>();

    SavedState() {}

    SavedState(CompoundTag nbt) {
      ListTag loaders = ic2.compat.NbtCompat.getList(nbt, "loaders", 10);

      for (int i = 0; i < loaders.size(); i++) {
        CompoundTag contentTag = loaders.getCompoundOrEmpty(i);
        BlockPos pos =
            new BlockPos(
                contentTag.getIntOr("x", 0),
                contentTag.getIntOr("y", 0),
                contentTag.getIntOr("z", 0));
        ((Set)
                this.chunksToChunkLoaders.computeIfAbsent(
                    ChunkPos.pack(pos), ignore -> new ObjectOpenHashSet<>(1)))
            .add(pos);
      }
    }

    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
      ListTag loaders = new ListTag();
      nbt.put("loaders", loaders);

      for (Set<BlockPos> positions : this.chunksToChunkLoaders.values()) {
        for (BlockPos pos : positions) {
          CompoundTag contentTag = new CompoundTag();
          loaders.add(contentTag);
          contentTag.putInt("x", pos.getX());
          contentTag.putInt("y", pos.getY());
          contentTag.putInt("z", pos.getZ());
        }
      }

      return nbt;
    }
  }
}
