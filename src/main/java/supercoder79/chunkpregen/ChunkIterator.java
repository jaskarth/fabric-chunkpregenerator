package supercoder79.chunkpregen;

import net.minecraft.util.math.ChunkPos;

public final class ChunkIterator {
    private final int originX;
    private final int originZ;
    private final int radius;

    private int x;
    private int z;

    private int currentRadius = 0;
    private int currentEdge;

    public ChunkIterator(int originX, int originZ, int radius) {
        this.originX = originX;
        this.originZ = originZ;
        this.radius = radius;
    }

    public long next() {
        if (this.currentRadius > this.radius) {
            return Long.MAX_VALUE;
        }

        long chunk = ChunkPos.toLong(this.x + this.originX, this.z + this.originZ);

        if (this.currentRadius > 0) {
            switch (this.currentEdge) {
                case 0:
                    if (Math.abs(++this.x) >= this.currentRadius) {
                        this.currentEdge++;
                    }
                    break;
                case 1:
                    if (Math.abs(++this.z) >= this.currentRadius) {
                        this.currentEdge++;
                    }
                    break;
                case 2:
                    if (Math.abs(--this.x) >= this.currentRadius) {
                        this.currentEdge++;
                    }
                    break;
                case 3:
                    if (Math.abs(--this.z) >= this.currentRadius) {
                        this.currentEdge = 0;
                        this.currentRadius++;
                    }
                    break;
            }
        } else {
            this.currentRadius++;
        }

        return chunk;
    }
}
