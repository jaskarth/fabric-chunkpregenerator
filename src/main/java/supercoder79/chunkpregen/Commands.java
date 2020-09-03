package supercoder79.chunkpregen;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.text.DecimalFormat;

public class Commands {
	private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.00");
	private static PregenerationTask activeTask;
	private static PregenBar pregenBar;

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			LiteralArgumentBuilder<ServerCommandSource> lab = CommandManager.literal("pregen")
					.requires(executor -> executor.hasPermissionLevel(2));

			lab.then(CommandManager.literal("start")
					.then(CommandManager.argument("radius", IntegerArgumentType.integer(0))
							.executes(cmd -> {

				ServerCommandSource source = cmd.getSource();
				if (activeTask != null) {
					source.sendFeedback(new LiteralText("Pregen already running. Please execute '/pregen stop' to start another pregeneration."), true);
					return Command.SINGLE_SUCCESS;
				}

				int radius = IntegerArgumentType.getInteger(cmd, "radius");

				ChunkPos origin;
				if (source.getEntity() == null) {
					origin = new ChunkPos(0, 0);
				} else {
					origin = new ChunkPos(new BlockPos(source.getPlayer().getPos()));
				}

				activeTask = new PregenerationTask(source.getWorld(), origin.x, origin.z, radius);
				pregenBar = new PregenBar();

				if (source.getEntity() instanceof ServerPlayerEntity) {
					pregenBar.addPlayer(source.getPlayer());
				}

				source.sendFeedback(new LiteralText("Pregenerating " + activeTask.getTotalCount() + " chunks..."), true);

				activeTask.run(createPregenListener(source));

				return Command.SINGLE_SUCCESS;
			})));

			lab.then(CommandManager.literal("stop")
					.executes(cmd -> {
				if (activeTask != null) {
					activeTask.stop();

					int count = activeTask.getOkCount() + activeTask.getErrorCount();
					int total = activeTask.getTotalCount();

					double percent = (double) count / total * 100.0;
					String message = "Pregen stopped! " + count + " out of " + total + " chunks generated. (" + percent + "%)";
					cmd.getSource().sendFeedback(new LiteralText(message), true);

					pregenBar.close();
					pregenBar = null;
					activeTask = null;
				}
				return Command.SINGLE_SUCCESS;
			}));

			lab.then(CommandManager.literal("status")
					.executes(cmd -> {
				if (activeTask != null) {
					int count = activeTask.getOkCount() + activeTask.getErrorCount();
					int total = activeTask.getTotalCount();

					double percent = (double) count / total * 100.0;
					String message = "Pregen status: " + count + " out of " + total + " chunks generated. (" + percent + "%)";
					cmd.getSource().sendFeedback(new LiteralText(message), true);
				} else {
					cmd.getSource().sendFeedback(new LiteralText("No pregeneration currently running. Run /pregen start <radius> to start."), false);
				}
				return Command.SINGLE_SUCCESS;
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

	private static PregenerationTask.Listener createPregenListener(ServerCommandSource source) {
		return new PregenerationTask.Listener() {
			@Override
			public void update(int ok, int error, int total) {
				int count = ok + error;
				if (count % 100 == 0) {
					pregenBar.update(ok, error, total);
				}
			}

			@Override
			public void complete(int error) {
				source.sendFeedback(new LiteralText("Pregeneration Done!"), true);

				if (error > 0) {
					source.sendFeedback(new LiteralText("Pregeneration experienced " + error + " errors! Check the log for more information"), true);
				}

				pregenBar.close();
				pregenBar = null;
				activeTask = null;
			}
		};
	}
}
