package supercoder79.chunkpregen;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Commands {
	private static int threadsDone = 0;
	private static ConcurrentLinkedQueue<ChunkPos> queue = new ConcurrentLinkedQueue<>();
	private static int total = 1;
	private static boolean shouldGenerate = false;
	private static ExecutorService executor;

	public static void init() {
        executor = Executors.newCachedThreadPool();
		CommandRegistry.INSTANCE.register(false, dispatcher -> {
			LiteralArgumentBuilder<ServerCommandSource> lab = CommandManager.literal("pregen")
					.requires(executor -> executor.hasPermissionLevel(2));

			lab.then(CommandManager.literal("start")
					.then(CommandManager.argument("radius", IntegerArgumentType.integer(0))
							.executes(cmd -> {
				if (!shouldGenerate) {
					shouldGenerate = true;

					ServerCommandSource source = cmd.getSource();

					int radius = IntegerArgumentType.getInteger(cmd, "radius");

					ChunkPos pos = new ChunkPos(new BlockPos(source.getPlayer().getPos()));

					queue.clear();

					total = 0;

					for (int x = pos.x - radius; x < pos.x + radius; x++) {
						for (int z = pos.z - radius; z < pos.z + radius; z++) {
							queue.add(new ChunkPos(x, z));
							total++;
						}
					}

					execute(source);

					source.sendFeedback(new LiteralText("Pregenerating " + total + " chunks..."), true);
				} else {
					cmd.getSource().sendFeedback(new LiteralText("Pregen already running. Please execute '/pregen stop' to start another pregeneration."), true);
				}
				return 1;
			})));

			lab.then(CommandManager.literal("stop")
					.executes(cmd -> {
				if (shouldGenerate) {
					int amount = total-queue.size();
					cmd.getSource().sendFeedback(new LiteralText("Pregen stopped! " + (amount) + " out of " + total + " generated. (" + (((double)(amount) / (double)(total))) * 100 + "%)"), true);
				}
				shouldGenerate = false;
				return 1;
			}));

			lab.then(CommandManager.literal("status")
					.executes(cmd -> {
				if (shouldGenerate) {
					int amount = total-queue.size();
					cmd.getSource().sendFeedback(new LiteralText("Pregen status: " + (amount) + " out of " + total + " generated. (" + (((double)(amount) / (double)(total))) * 100 + "%)"), true);
				} else {
					cmd.getSource().sendFeedback(new LiteralText("No pregeneration currently running. Run /pregen start <radius> to start."), false);
				}
				return 1;
			}));

			lab.then(CommandManager.literal("help").
					executes(cmd -> {
				ServerCommandSource source = cmd.getSource();

				source.sendFeedback(new LiteralText("/pregen start <radius> - Pregenerate a square centered on the player that is <radius> chunks long and wide."), false);
				source.sendFeedback(new LiteralText("/pregen stop - Stop pregeneration and displays the amount completed."), false);
				source.sendFeedback(new LiteralText("/pregen status - Display the amount of chunks pregenerated."), false);
				source.sendFeedback(new LiteralText("/pregen help - Display this message."), false);
				return 1;
			}));

			dispatcher.register(lab);
		});
	}

	private static void incrementAmount(ServerCommandSource source) {
		int amount = total - queue.size();

		if (amount % 100 == 0) {
			source.sendFeedback(new LiteralText("Pregenerated " + (((double)(amount) / (double)(total))) * 100 + "%"), true);
		}
	}

	private static void finishThread(ServerCommandSource source) {
		threadsDone++;
		if (threadsDone == 4) {
			threadsDone = 0;
			source.sendFeedback(new LiteralText("Pregeneration Done!"), true);
			shouldGenerate = false;
		}
	}

    private static void execute(ServerCommandSource source) {
        threadsDone = 0;

        for (int i = 0; i < 4; i++) {
            executor.submit(new ChunkWorker(source));
        }
    }

	static class ChunkWorker implements Runnable {
		private ServerCommandSource source;

		ChunkWorker(ServerCommandSource source) {
			this.source = source;
		}

		@Override
		public void run() {
			ServerWorld world = source.getWorld();

			while (shouldGenerate) {
				ChunkPos pos = queue.poll();
				if (pos == null) break;

				world.getChunk(pos.x, pos.z);
				incrementAmount(source);
			}

			finishThread(source);
		}
	}
}
