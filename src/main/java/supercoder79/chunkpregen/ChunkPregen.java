package supercoder79.chunkpregen;

import net.fabricmc.api.ModInitializer;

public class ChunkPregen implements ModInitializer {
	@Override
	public void onInitialize() {
		Commands.init();
	}
}
