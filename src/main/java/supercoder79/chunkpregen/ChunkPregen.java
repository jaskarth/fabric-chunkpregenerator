package supercoder79.chunkpregen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import supercoder79.chunkpregen.client.GoVote;

public final class ChunkPregen implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(ChunkPregen.class);

	@Override
	public void onInitialize() {
		Commands.init();

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			GoVote.init();
		}
	}
}
