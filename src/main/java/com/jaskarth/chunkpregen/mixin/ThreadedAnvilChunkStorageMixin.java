package com.jaskarth.chunkpregen.mixin;

import com.jaskarth.chunkpregen.ChunkPregen;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkLoadingManager.class)
public class ThreadedAnvilChunkStorageMixin {

    @Final
    @Shadow
    private Long2ByteMap chunkToType;

    @Final
    @Shadow
    private PointOfInterestStorage pointOfInterestStorage;

    @Inject(method = "method_60440(Lnet/minecraft/server/world/ChunkHolder;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkLoadingManager;save(Lnet/minecraft/world/chunk/Chunk;)Z"))
    private void fabricChunkPregenerator$unloadChunkPOI(ChunkHolder chunkHolder, long chunkLong, CallbackInfo ci) {
        this.chunkToType.remove(chunkLong);
        ChunkPregen.onChunkUnload(pointOfInterestStorage, chunkHolder.getLatest());
    }
}
