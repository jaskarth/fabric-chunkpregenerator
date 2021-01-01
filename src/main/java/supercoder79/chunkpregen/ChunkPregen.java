package supercoder79.chunkpregen;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public final class ChunkPregen implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(ChunkPregen.class);
	@Override
	public void onInitialize() {
		Commands.init();
	}
}
