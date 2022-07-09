package supercoder79.chunkpregen;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;

public final class PregenBar implements AutoCloseable {
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("#.00");

    private final ServerBossBar bar;

    public PregenBar() {
        this.bar = new ServerBossBar(Text.literal("Pregenerating"), BossBar.Color.BLUE, BossBar.Style.PROGRESS);
        this.bar.setDragonMusic(false);
        this.bar.setThickenFog(false);
        this.bar.setDarkenSky(false);
    }

    public void update(int ok, int error, int total) {
        int count = ok + error;

        float percent = (float) count / total;

        MutableText title = Text.literal("Pregenerating " + total + " chunks! ")
                .append(Text.literal(PERCENT_FORMAT.format(percent * 100.0F) + "%").formatted(Formatting.AQUA));

        if (error > 0) {
            title = title.append(Text.literal(" (" + error + " errors!)").formatted(Formatting.RED));
        }

        this.bar.setName(title);
        this.bar.setPercent(percent);
    }

    public void addPlayer(ServerPlayerEntity player) {
        this.bar.addPlayer(player);
    }

    @Override
    public void close() {
        this.bar.setVisible(false);
        this.bar.clearPlayers();
    }
}
