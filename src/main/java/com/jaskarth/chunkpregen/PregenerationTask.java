package com.jaskarth.chunkpregen;

import com.jaskarth.chunkpregen.iterator.CoarseOnionIterator;
import com.jaskarth.chunkpregen.mixin.ThreadedAnvilChunkStorageAccessor;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.scanner.NbtScanQuery;
import net.minecraft.nbt.scanner.SelectiveNbtCollector;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.*;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class PregenerationTask {
    private static final int BATCH_SIZE = 32;
    private static final int QUEUE_THRESHOLD = 8;
    private static final int COARSE_CELL_SIZE = 4;

    private final MinecraftServer server;
    private final ServerChunkManager chunkManager;
    private final ServerWorld serverLevel;

    private final Iterator<ChunkPos> iterator;
    private final int x;
    private final int z;
    private final int radius;

    private final int totalCount;

    private final Object queueLock = new Object();

    private final AtomicInteger queuedCount = new AtomicInteger();
    private final AtomicInteger okCount = new AtomicInteger();
    private final AtomicInteger errorCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();

    private volatile Listener listener;
    private volatile boolean stopped;

    public static final ChunkTicketType<ChunkPos> FABRIC_PREGEN_FORCED = ChunkTicketType.create("fabric_pregen_forced", Comparator.comparingLong(ChunkPos::toLong));

    public PregenerationTask(ServerWorld world, int x, int z, int radius) {
        this.server = world.getServer();
        this.chunkManager = world.getChunkManager();
        this.serverLevel = world;

        this.iterator = new CoarseOnionIterator(radius, COARSE_CELL_SIZE);
        this.x = x;
        this.z = z;
        this.radius = radius;

        int diameter = radius * 2 + 1;
        this.totalCount = diameter * diameter;
    }

    public int getOkCount() {
        return this.okCount.get();
    }

    public int getErrorCount() {
        return this.errorCount.get();
    }

    public int getSkippedCount() {
        return this.skippedCount.get();
    }

    public int getTotalCount() {
        return this.totalCount;
    }

    public void run(Listener listener) {
        if (this.listener != null) {
            throw new IllegalStateException("already running!");
        }

        this.listener = listener;

        // Off thread chunk scanning to skip already generated chunks
        CompletableFuture.runAsync(this::tryEnqueueTasks, Util.getMainWorkerExecutor());
    }

    public void stop() {
        synchronized (this.queueLock) {
            this.stopped = true;
            this.listener = null;
        }
    }

    private void tryEnqueueTasks() {
        synchronized (this.queueLock) {
            if (this.stopped) {
                return;
            }

            int enqueueCount = BATCH_SIZE - this.queuedCount.get();
            if (enqueueCount <= 0) {
                return;
            }

            LongList chunks = this.collectChunks(enqueueCount);
            if (chunks.isEmpty()) {
                this.listener.complete(this.errorCount.get());
                this.stopped = true;
                return;
            }

            this.queuedCount.getAndAdd(chunks.size());

            // Keep on server thread as chunk acquiring and releasing (tickets) is not thread safe.
            this.server.submit(() -> this.enqueueChunks(chunks));
        }
    }

    private void enqueueChunks(LongList chunks) {
        for (int i = 0; i < chunks.size(); i++) {
            long chunk = chunks.getLong(i);
            this.acquireChunk(chunk);
        }

        // tick the chunk manager to force the ChunkHolders to be created
        this.chunkManager.tick(() -> true, true);

        ServerChunkLoadingManager tacs = this.chunkManager.chunkLoadingManager;

        for (int i = 0; i < chunks.size(); i++) {
            long chunk = chunks.getLong(i);

            ChunkHolder holder = tacs.getChunkHolder(chunk);
            if (holder == null) {
                ChunkPregen.LOGGER.warn("Added ticket for chunk but it was not added! ({}; {})", ChunkPos.getPackedX(chunk), ChunkPos.getPackedZ(chunk));
                this.acceptChunkResult(chunk, ChunkHolder.UNLOADED_WORLD_CHUNK);
                continue;
            }

            holder.getAccessibleFuture()

//            holder.getChunkAt(ChunkStatus.FULL, tacs)
                    .whenCompleteAsync((result, throwable) -> {
                if (throwable == null) {
                    this.acceptChunkResult(chunk, result);
                } else {
                    ChunkPregen.LOGGER.warn("Encountered unexpected error while generating chunk", throwable);
                    this.acceptChunkResult(chunk, ChunkHolder.UNLOADED_WORLD_CHUNK);
                }
            }, runnable -> ((ThreadedAnvilChunkStorageAccessor)tacs).getMainExecutor().send(ChunkTaskPrioritySystem.createMessage(holder, runnable)));
        }
    }

    private void acceptChunkResult(long chunk, OptionalChunk<WorldChunk> result) {
        this.server.submit(() -> this.releaseChunk(chunk));

        if (result.isPresent()) {
            this.okCount.getAndIncrement();
        } else {
            this.errorCount.getAndIncrement();
        }

        this.listener.update(this.okCount.get(), this.errorCount.get(), this.skippedCount.get(), this.totalCount);

        int queuedCount = this.queuedCount.decrementAndGet();
        if (queuedCount <= QUEUE_THRESHOLD) {
            this.tryEnqueueTasks();
        }
    }

    private LongList collectChunks(int count) {
        LongList chunks = new LongArrayList(count);

        Iterator<ChunkPos> iterator = this.iterator;
        for (int i = 0; i < count && iterator.hasNext();) {
            ChunkPos chunkPosInLocalSpace = iterator.next();
            if (isChunkFullyGenerated(chunkPosInLocalSpace)) {
                this.skippedCount.incrementAndGet();
                this.listener.update(this.okCount.get(), this.errorCount.get(), this.skippedCount.get(), this.totalCount);
                continue;
            }

            chunks.add(ChunkPos.toLong(chunkPosInLocalSpace.x + this.x, chunkPosInLocalSpace.z + this.z));
            i++;
        }

        return chunks;
    }

    private void acquireChunk(long chunk) {
        ChunkPos pos = new ChunkPos(chunk);
        this.chunkManager.addTicket(FABRIC_PREGEN_FORCED, pos, 0, pos);
    }

    private void releaseChunk(long chunk) {
        ChunkPos pos = new ChunkPos(chunk);
        this.chunkManager.removeTicket(FABRIC_PREGEN_FORCED, pos, 0, pos);
    }

    private boolean isChunkFullyGenerated(ChunkPos chunkPosInLocalSpace) {
        ChunkPos chunkPosInWorldSpace = new ChunkPos(chunkPosInLocalSpace.x + this.x, chunkPosInLocalSpace.z + this.z);
        SelectiveNbtCollector nbtCollector = new SelectiveNbtCollector(new NbtScanQuery(NbtString.TYPE, "Status"));
        this.chunkManager.getChunkIoWorker().scanChunk(chunkPosInWorldSpace, nbtCollector).join();

        if (nbtCollector.getRoot() instanceof NbtCompound nbtCompound) {
            return nbtCompound.getString("Status").equals("minecraft:full");
        }

        return false;
    }

    public interface Listener {
        void update(int ok, int error, int skipped, int total);

        void complete(int error);
    }
}
