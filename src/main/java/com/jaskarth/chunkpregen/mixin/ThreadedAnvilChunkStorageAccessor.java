package com.jaskarth.chunkpregen.mixin;

import net.minecraft.server.world.ChunkTaskPrioritySystem;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerChunkLoadingManager.class)
public interface ThreadedAnvilChunkStorageAccessor {
    @Accessor
    MessageListener<ChunkTaskPrioritySystem.Task<Runnable>> getMainExecutor();
}
