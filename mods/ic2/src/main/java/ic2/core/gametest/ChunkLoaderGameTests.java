package ic2.core.gametest;

import ic2.compat.GameTest;
import ic2.core.block.machine.tileentity.TileEntityChunkLoader;
import ic2.core.event.WorldData;
import ic2.core.ref.Ic2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ChunkLoaderGameTests {
  private static final String EMPTY = "gametest/empty3x3x3";

  private static final BlockPos LOADER_POS = new BlockPos(1, 1, 1);

  // chunk loader: consumes euPerChunk (default 1 EU/t) per loaded chunk and keeps the chunks
  // force-loaded while powered
  @GameTest(template = EMPTY, timeoutTicks = 200)
  public static void chunkLoaderForceLoadsWhilePoweredAndStopsWhenDrained(GameTestHelper helper) {
    TileEntityChunkLoader te = placeLoader(helper);
    long ownChunk = ChunkPos.pack(helper.absolutePos(LOADER_POS));
    te.energy.addEnergy(50.0);

    helper.runAtTickTime(
        10,
        () -> {
          helper.assertTrue(te.getActive(), "a powered chunk loader should be active");
          helper.assertTrue(
              te.energy.getEnergy() < 50.0,
              "the chunk loader should consume 1 EU/t per chunk, has " + te.energy.getEnergy());
          WorldData worldData = WorldData.get(helper.getLevel());
          helper.assertTrue(
              worldData.loadedChunks.containsKey(ownChunk),
              "the loader's own chunk should be registered as force-loaded");
        });

    helper.runAtTickTime(
        120,
        () -> {
          helper.assertFalse(
              te.getActive(), "the chunk loader must shut down once its 50 EU are used up");
          WorldData worldData = WorldData.get(helper.getLevel());
          helper.assertFalse(
              worldData.loadedChunks.containsKey(ownChunk),
              "the chunk must be released when the loader shuts down");
          helper.succeed();
        });
  }

  @GameTest(template = EMPTY)
  public static void chunkLoaderOnlyAcceptsChunksInRange(GameTestHelper helper) {
    TileEntityChunkLoader te = placeLoader(helper);
    ChunkPos mainChunk = ChunkPos.containing(helper.absolutePos(LOADER_POS));
    ChunkPos nearChunk = new ChunkPos(mainChunk.x() + 4, mainChunk.z());
    ChunkPos farChunk = new ChunkPos(mainChunk.x() + 5, mainChunk.z());

    helper.assertTrue(
        te.isChunkInRange(nearChunk), "a chunk 4 chunks away is within the 4 chunk radius");
    helper.assertFalse(
        te.isChunkInRange(farChunk), "a chunk 5 chunks away is outside the 4 chunk radius");

    te.addChunkToLoaded(nearChunk);
    te.addChunkToLoaded(farChunk);
    helper.assertValueEqual(te.getLoadedChunks().size(), 2, "loaded chunk entries (own + near)");
    helper.assertTrue(
        te.getLoadedChunks().contains(nearChunk.pack()), "the in-range chunk should be accepted");
    helper.assertFalse(
        te.getLoadedChunks().contains(farChunk.pack()), "the out-of-range chunk must be rejected");

    helper.assertFalse(
        WorldData.get(helper.getLevel()).chunkLoaders.containsKey(helper.absolutePos(LOADER_POS)),
        "changing an unpowered loader's selection must not register chunk tickets");
    helper.succeed();
  }

  private static TileEntityChunkLoader placeLoader(GameTestHelper helper) {
    helper.setBlock(LOADER_POS, Ic2Blocks.CHUNK_LOADER);
    TileEntityChunkLoader te = getTe(helper, LOADER_POS, TileEntityChunkLoader.class);
    // placing via setBlock skips onPlaced, so register the loader's own chunk like item placement
    // would
    te.getLoadedChunks().add(ChunkPos.pack(helper.absolutePos(LOADER_POS)));
    return te;
  }

  private static <T extends BlockEntity> T getTe(
      GameTestHelper helper, BlockPos pos, Class<T> type) {
    BlockEntity be =
        helper.getBlockEntity(pos, net.minecraft.world.level.block.entity.BlockEntity.class);
    if (!type.isInstance(be)) {
      throw new IllegalStateException(
          "expected " + type.getSimpleName() + " at " + pos + ", found " + be);
    }

    return type.cast(be);
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void auditNetworkSelectionEnforcesNineChunkLimit(GameTestHelper h) {
    var loader = placeLoader(h);
    var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    for (int x = -4; x <= 4; x++)
      for (int z = -4; z <= 4; z++) {
        if (x == 0 && z == 0) continue;
        loader.onNetworkEvent(player, (x + 8) & 15 | ((z + 8) & 15) << 4);
      }
    h.assertTrue(
        loader.getLoadedChunks().size() <= loader.getMaxChunks(),
        "server must enforce max="
            + loader.getMaxChunks()
            + "; actually selected="
            + loader.getLoadedChunks().size());
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void auditRemovingSiblingLoaderMarksSavedStateDirty(GameTestHelper h)
      throws Exception {
    var positions = auditPositions(h, 1);
    var own = net.minecraft.world.level.ChunkPos.pack(positions[0]);
    ic2.core.ChunkLoaderLogic.addChunkLoader(
        h.getLevel(),
        positions[0],
        new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {own}));
    ic2.core.ChunkLoaderLogic.addChunkLoader(
        h.getLevel(),
        positions[1],
        new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {own}));
    try {
      var state = auditState(h);
      state.setDirty(false);
      ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), positions[0]);
      h.assertTrue(
          state.isDirty(),
          "removing one of two loaders in the same chunk must mark saved data dirty");
    } finally {
      ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), positions[0]);
      ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), positions[1]);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  @SuppressWarnings({"unchecked", "rawtypes"})
  public static void auditRemovedSiblingDoesNotReappearAfterRealDiskReload(GameTestHelper h)
      throws Exception {
    var positions = auditPositions(h, 2);
    long own = net.minecraft.world.level.ChunkPos.pack(positions[0]);
    for (var pos : positions)
      ic2.core.ChunkLoaderLogic.addChunkLoader(
          h.getLevel(), pos, new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {own}));
    try {
      var type = auditType();
      var state = auditState(h);
      var path = auditOutput().resolve("saved-data-roundtrip");
      java.nio.file.Files.createDirectories(path);
      try (var disk =
          new net.minecraft.world.level.storage.SavedDataStorage(
              path,
              net.minecraft.util.datafix.DataFixers.getDataFixer(),
              h.getLevel().registryAccess())) {
        disk.set(type, state);
        disk.saveAndJoin();
        h.assertFalse(state.isDirty(), "actual save clears dirty flag");
        ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), positions[0]);
        disk.saveAndJoin();
      }
      try (var disk =
          new net.minecraft.world.level.storage.SavedDataStorage(
              path,
              net.minecraft.util.datafix.DataFixers.getDataFixer(),
              h.getLevel().registryAccess())) {
        var restored = disk.get(type);
        var f = restored.getClass().getDeclaredField("chunksToChunkLoaders");
        f.setAccessible(true);
        var index =
            (it.unimi.dsi.fastutil.longs.Long2ObjectMap<java.util.Set<BlockPos>>) f.get(restored);
        boolean ghost = index.get(own) != null && index.get(own).contains(positions[0]);
        java.nio.file.Files.writeString(
            auditOutput().resolve("disk-roundtrip-observation.json"),
            "{\"removed_loader_reappeared\":"
                + ghost
                + ",\"remaining_loader_present\":"
                + (index.get(own) != null && index.get(own).contains(positions[1]))
                + "}");
        h.assertFalse(
            ghost, "removed loader reappeared after native SavedDataStorage save/close/reopen");
      }
    } finally {
      for (var pos : positions) ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), pos);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void auditSharedTicketsStayUntilLastOwnerStops(GameTestHelper h) {
    var positions = auditPositions(h, 3);
    long own = net.minecraft.world.level.ChunkPos.pack(positions[0]);
    long target =
        new net.minecraft.world.level.ChunkPos(
                net.minecraft.world.level.ChunkPos.containing(positions[0]).x() + 1,
                net.minecraft.world.level.ChunkPos.containing(positions[0]).z())
            .pack();
    var selection = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {own, target});
    for (var pos : positions)
      ic2.core.ChunkLoaderLogic.addChunkLoader(h.getLevel(), pos, selection);
    try {
      ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), positions[0]);
      h.assertTrue(auditHasTicket(h, target), "shared target ticket must remain for second owner");
      ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), positions[1]);
      h.assertFalse(auditHasTicket(h, target), "target ticket released after final owner stops");
    } finally {
      for (var pos : positions) ic2.core.ChunkLoaderLogic.removeChunkLoader(h.getLevel(), pos);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void auditLiveSelectionChangesUpdateNativeTickets(GameTestHelper h) {
    var loader = placeLoader(h);
    loader.energy.addEnergy(2000);
    loader.setActive(true);
    var own = net.minecraft.world.level.ChunkPos.containing(loader.getBlockPos());
    var selected = new net.minecraft.world.level.ChunkPos(own.x() + 4, own.z() + 4);
    try {
      loader.addChunkToLoaded(selected);
      h.assertTrue(auditHasTicket(h, selected.pack()), "selected chunk has native IC2 ticket");
      loader.removeChunkFromLoaded(selected);
      h.assertFalse(auditHasTicket(h, selected.pack()), "deselected chunk loses native IC2 ticket");
      loader.removeChunkFromLoaded(own);
      h.assertTrue(loader.getLoadedChunks().contains(own.pack()), "own chunk cannot be removed");
    } finally {
      loader.setActive(false);
    }
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit", timeoutTicks = 100)
  public static void auditBreakingLoaderReleasesNativeTickets(GameTestHelper h) {
    var loader = placeLoader(h);
    loader.energy.addEnergy(2000);
    var main = net.minecraft.world.level.ChunkPos.containing(loader.getBlockPos());
    var remote = new net.minecraft.world.level.ChunkPos(main.x() + 4, main.z() + 4);
    loader.addChunkToLoaded(remote);
    loader.setActive(true);
    h.runAtTickTime(
        5,
        () -> {
          h.assertTrue(auditHasTicket(h, remote.pack()), "remote ticket active before destruction");
          h.getLevel().destroyBlock(loader.getBlockPos(), true);
          h.assertFalse(auditHasTicket(h, remote.pack()), "destruction releases remote ticket");
          h.assertFalse(
              WorldData.get(h.getLevel()).chunkLoaders.containsKey(loader.getBlockPos()),
              "runtime loader index removed");
          h.succeed();
        });
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit", timeoutTicks = 350)
  public static void auditRemoteFurnaceRunsOutsidePlayerSimulation(GameTestHelper h) {
    // Keep the fixture away from other suites' fake players and temporary chunk tickets.
    BlockPos loaderPos = auditPositions(h, 100)[0];
    var main = ChunkPos.containing(loaderPos);
    h.getLevel().getChunkSource().updateChunkForced(main, true);
    h.getLevel().setBlockAndUpdate(loaderPos, Ic2Blocks.CHUNK_LOADER.defaultBlockState());
    var loader = (TileEntityChunkLoader) h.getLevel().getBlockEntity(loaderPos);
    loader.getLoadedChunks().add(main.pack());
    var remote = new net.minecraft.world.level.ChunkPos(main.x() + 4, main.z() + 4);
    BlockPos target = new BlockPos(remote.getMinBlockX() + 8, 80, remote.getMinBlockZ() + 8);
    var furnace = auditFurnace(h, target, 1);
    loader.addChunkToLoaded(remote);
    h.runAtTickTime(
        20,
        () -> {
          // Other suites create fake players near spawn. They must not help simulate this
          // remote machine, but their mere existence elsewhere is not a loader failure.
          h.assertTrue(
              h.getLevel().players().stream()
                  .allMatch(player -> player.blockPosition().distSqr(target) > 1024.0 * 1024.0),
              "no player within 1024 blocks of the remote machine");
          h.assertFalse(
              h.getLevel().shouldTickBlocksAt(remote.pack()),
              "remote chunk must not already be simulated by GameTest");
          h.assertTrue(
              furnace.outputSlot.get(0).isEmpty(), "unforced remote machine has not smelted");
          loader.energy.addEnergy(2500);
        });
    h.runAtTickTime(30, () -> h.getLevel().getChunkSource().updateChunkForced(main, false));
    h.succeedWhen(
        () -> {
          h.assertTrue(h.getTick() > 25, "wait for ticket propagation");
          h.assertTrue(
              h.getLevel().shouldTickBlocksAt(remote.pack()),
              "IC2 ticket enables real block simulation");
          h.assertTrue(
              furnace.outputSlot.get(0).is(net.minecraft.world.item.Items.IRON_INGOT),
              "remote furnace actually smelts outside player simulation");
          h.assertValueEqual(furnace.outputSlot.get(0).getCount(), 1, "one iron output");
          try {
            java.nio.file.Files.writeString(
                auditOutput().resolve("remote-no-player-observation.json"),
                "{\"players\":"
                    + h.getLevel().getServer().getPlayerList().getPlayerCount()
                    + ",\"block_ticking_before_power\":false,\"block_ticking_after_power\":true,\"native_ic2_ticket\":"
                    + auditHasTicket(h, remote.pack())
                    + ",\"iron_ingot_output\":1}");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
          loader.setActive(false);
          h.getLevel().getChunkSource().updateChunkForced(main, false);
        });
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void auditSelectionAndPowerSurviveNbtRoundtrip(GameTestHelper h) {
    var loader = placeLoader(h);
    var main = net.minecraft.world.level.ChunkPos.containing(loader.getBlockPos());
    loader.addChunkToLoaded(new net.minecraft.world.level.ChunkPos(main.x() + 4, main.z() - 4));
    loader.energy.addEnergy(1500);
    loader.setActive(true);
    var nbt = loader.saveWithFullMetadata(h.getLevel().registryAccess());
    var copy = new TileEntityChunkLoader(loader.getBlockPos(), loader.getBlockState());
    copy.loadWithComponents(nbt, h.getLevel().registryAccess());
    h.assertTrue(
        copy.getLoadedChunks().equals(loader.getLoadedChunks()), "selected chunks retained in NBT");
    h.assertValueEqual((int) copy.energy.getEnergy(), 1500, "power retained in NBT");
    h.assertTrue(copy.getActive(), "active flag retained in NBT");
    loader.setActive(false);
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void selectionAtCapacityAllowsRemovingAndReplacing(GameTestHelper h) {
    var loader = placeLoader(h);
    var own = ChunkPos.containing(loader.getBlockPos());
    var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        loader.addChunkToLoaded(new ChunkPos(own.x() + x, own.z() + z));
      }
    }
    var replacement = new ChunkPos(own.x() + 4, own.z() + 4);
    loader.addChunkToLoaded(replacement);
    h.assertValueEqual(loader.getLoadedChunks().size(), 9, "full selection rejects new cell");
    h.assertFalse(loader.getLoadedChunks().contains(replacement.pack()), "rejected cell absent");
    loader.onNetworkEvent(player, 7 | 7 << 4);
    loader.onNetworkEvent(player, 12 | 12 << 4);
    h.assertValueEqual(loader.getLoadedChunks().size(), 9, "replacement fills freed slot");
    h.assertTrue(loader.getLoadedChunks().contains(replacement.pack()), "new cell accepted");
    loader.onNetworkEvent(player, 8 | 8 << 4);
    h.assertTrue(loader.getLoadedChunks().contains(own.pack()), "own cell remains at capacity");
    h.succeed();
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void oldOversizedSelectionKeepsOwnAndNearestChunks(GameTestHelper h) {
    var loader = placeLoader(h);
    var own = ChunkPos.containing(loader.getBlockPos());
    var nbt = loader.saveWithFullMetadata(h.getLevel().registryAccess());
    var oldSelection = new net.minecraft.nbt.ListTag();
    for (int x = -4; x <= 4; x++) {
      for (int z = -4; z <= 4; z++) {
        if (x != 0 || z != 0) {
          oldSelection.add(
              net.minecraft.nbt.LongTag.valueOf(new ChunkPos(own.x() + x, own.z() + z).pack()));
        }
      }
    }
    oldSelection.add(net.minecraft.nbt.LongTag.valueOf(new ChunkPos(own.x() + 99, own.z()).pack()));
    nbt.put("loadedChunks", oldSelection);
    loader.loadWithComponents(nbt, h.getLevel().registryAccess());
    h.runAtTickTime(
        5,
        () -> {
          h.assertValueEqual(
              loader.getLoadedChunks().size(), 9, "old oversized selection normalized");
          for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
              h.assertTrue(
                  loader.getLoadedChunks().contains(new ChunkPos(own.x() + x, own.z() + z).pack()),
                  "nearest 3x3 retained including missing own cell");
            }
          }
          h.succeed();
        });
  }

  @GameTest(template = EMPTY, batch = "chunk_loader_audit")
  public static void chunkLoadRemovesGhostAndInactiveRecordsKeepsPoweredLoader(GameTestHelper h)
      throws Exception {
    var loader = placeLoader(h);
    loader.energy.addEnergy(2000);
    loader.setActive(true);
    var own = ChunkPos.containing(loader.getBlockPos());
    BlockPos ghost = loader.getBlockPos().offset(1, 0, 0);
    BlockPos inactive = loader.getBlockPos().offset(0, 1, 0);
    h.getLevel().setBlockAndUpdate(inactive, Ic2Blocks.CHUNK_LOADER.defaultBlockState());
    for (var pos : new BlockPos[] {ghost, inactive}) {
      ic2.core.ChunkLoaderLogic.addChunkLoader(
          h.getLevel(),
          pos,
          new it.unimi.dsi.fastutil.longs.LongOpenHashSet(new long[] {own.pack()}));
    }
    var state = auditState(h);
    state.setDirty(false);
    ic2.core.event.EventHandler.onChunkLoad(h.getLevel().getChunk(own.x(), own.z()));
    h.runAtTickTime(
        5,
        () -> {
          var data = WorldData.get(h.getLevel());
          h.assertFalse(data.chunkLoaders.containsKey(ghost), "nonexistent loader removed");
          h.assertFalse(data.chunkLoaders.containsKey(inactive), "inactive loader removed");
          h.assertTrue(
              data.chunkLoaders.containsKey(loader.getBlockPos()), "powered loader retained");
          h.assertTrue(auditHasTicket(h, own.pack()), "valid owner keeps chunk ticket");
          h.assertTrue(state.isDirty(), "stale record cleanup persisted");
          loader.setActive(false);
          h.succeed();
        });
  }

  private static BlockPos[] auditPositions(GameTestHelper h, int salt) {
    var base = net.minecraft.world.level.ChunkPos.containing(h.absolutePos(LOADER_POS));
    int x = (base.x() + 100 + salt * 8) * 16, z = (base.z() + 100) * 16;
    return new BlockPos[] {new BlockPos(x + 1, 80, z + 1), new BlockPos(x + 2, 80, z + 1)};
  }

  @SuppressWarnings("rawtypes")
  private static net.minecraft.world.level.saveddata.SavedDataType auditType() throws Exception {
    var f = ic2.core.ChunkLoaderLogic.class.getDeclaredField("savedStateType");
    f.setAccessible(true);
    return (net.minecraft.world.level.saveddata.SavedDataType) f.get(null);
  }

  @SuppressWarnings("unchecked")
  private static net.minecraft.world.level.saveddata.SavedData auditState(GameTestHelper h)
      throws Exception {
    return h.getLevel().getDataStorage().computeIfAbsent(auditType());
  }

  private static boolean auditHasTicket(GameTestHelper h, long chunk) {
    return h
        .getLevel()
        .getDataStorage()
        .computeIfAbsent(net.minecraft.world.level.TicketStorage.TYPE)
        .getTickets(chunk)
        .stream()
        .anyMatch(ticket -> ticket.getType() == ic2.core.ChunkLoaderLogic.TICKET_TYPE);
  }

  private static java.nio.file.Path auditOutput() {
    var path =
        java.nio.file.Path.of(
            System.getProperty("ic2.chunk.audit.output", "build/chunk-loader-gametest"));
    try {
      java.nio.file.Files.createDirectories(path);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("Cannot create chunk loader test output", exception);
    }
    return path;
  }

  private static ic2.core.block.machine.tileentity.TileEntityElectricFurnace auditFurnace(
      GameTestHelper h, BlockPos pos, int count) {
    h.getLevel().setBlockAndUpdate(pos, Ic2Blocks.ELECTRIC_FURNACE.defaultBlockState());
    var furnace =
        (ic2.core.block.machine.tileentity.TileEntityElectricFurnace)
            h.getLevel().getBlockEntity(pos);
    furnace.getComponent(ic2.core.block.comp.Energy.class).addEnergy(1000);
    furnace.inputSlot.put(
        0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, count));
    return furnace;
  }
}
