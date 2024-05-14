package supercoder79.chunkpregen;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.poi.PointOfInterestSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import supercoder79.chunkpregen.mixin.SerializingRegionBasedStorageAccessor;

public final class ChunkPregen implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(ChunkPregen.class);

	@Override
	public void onInitialize() {
		Commands.init();
	}

	/**
	 * The goal here is to fix the POI memory leak that happens due to
	 * {@link net.minecraft.world.storage.SerializingRegionBasedStorage#loadedElements} field never
	 * actually removing POIs long after they become irrelevant. We do it here in chunk unloading
	 * so that chunk that are fully unloaded now gets the POI removed from the POI cached storage map.
	 */
	public static void onChunkUnload(PointOfInterestStorage pointOfInterestStorage, Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		pointOfInterestStorage.saveChunk(chunkPos); // Make sure all POI in chunk are saved to disk first.

		// Remove the cached POIs for this chunk's location.
		int SectionPosMinY = ChunkSectionPos.getSectionCoord(chunk.getBottomY());
		for (int currentSectionY = 0; currentSectionY < chunk.countVerticalSections(); currentSectionY++) {
			long sectionPosKey = ChunkSectionPos.asLong(chunkPos.x, SectionPosMinY + currentSectionY, chunkPos.z);
			((SerializingRegionBasedStorageAccessor<PointOfInterestSet>)pointOfInterestStorage).getLoadedElements().remove(sectionPosKey);
		}
	}
}
