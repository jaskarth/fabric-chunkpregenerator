package supercoder79.chunkpregen.iterator;

import net.minecraft.util.math.ChunkPos;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class OnionIterator implements Iterator<ChunkPos> {
    private static final byte EAST = 0;
    private static final byte SOUTH = 1;
    private static final byte WEST = 2;
    private static final byte NORTH = 3;
    private static final byte STOP = 4;

    private final int radius;

    private int x;
    private int z;

    private int distance = 0;
    private byte state = EAST;

    public OnionIterator(int radius) {
        this.radius = radius;
    }

    @Override
    public ChunkPos next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        ChunkPos pos = new ChunkPos(this.x, this.z);

        switch (this.state) {
            case EAST:
                if (++this.x >= this.distance) {
                    this.state = SOUTH;
                    if (this.distance > this.radius) {
                        this.state = STOP;
                    }
                }
                break;
            case SOUTH:
                if (++this.z >= this.distance) {
                    this.state = WEST;
                }
                break;
            case WEST:
                if (--this.x <= -this.distance) {
                    this.state = NORTH;
                }
                break;
            case NORTH:
                if (--this.z <= -this.distance) {
                    this.state = EAST;
                    this.distance++;
                }
                break;
        }

        if (this.distance == 0) {
            this.distance++;
        }

        return pos;
    }

    @Override
    public boolean hasNext() {
        return this.state != STOP;
    }
}
