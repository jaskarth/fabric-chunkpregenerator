package supercoder79.chunkpregen.mixin;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import supercoder79.chunkpregen.ChunkPregen;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Mixin(ThreadedAnvilChunkStorage.class)
public class ThreadedAnvilChunkStorageMixin {

    @Final
    @Shadow
    private Long2ByteMap chunkToType;

    @Final
    @Shadow
    private PointOfInterestStorage pointOfInterestStorage;

    @Inject(method = "method_18843(Lnet/minecraft/server/world/ChunkHolder;Ljava/util/concurrent/CompletableFuture;JLnet/minecraft/world/chunk/Chunk;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;save(Lnet/minecraft/world/chunk/Chunk;)Z"))
    private void fabricChunkPregenerator$unloadChunkPOI(ChunkHolder chunkHolder, CompletableFuture<Chunk> completableFuture, long chunkLong, Chunk chunk, CallbackInfo ci) {
        this.chunkToType.remove(chunkLong);
        ChunkPregen.onChunkUnload(pointOfInterestStorage, chunk);
    }
}
