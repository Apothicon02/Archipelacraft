package org.archipelacraft.game;

import org.archipelacraft.game.noise.Noises;

public class World {
    public static int size = 1024;
    public static int height = 320;
    public static short[] blocks = new short[World.size*World.size*World.height*4];

    public static int condensePos(int x, int y, int z) {
        return ((((x*height)+y)*size)+z)*4;
    }
    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < size && y >= 0 && y < height && z >= 0 && z < size);
    }

    public static void setBlock(int x, int y, int z, int block, int blockSubType) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z);
            blocks[pos] = (short)(block);
            blocks[pos+1] = (short)(blockSubType);
        }
    }
    public static void setBlock(float x, float y, float z, int block, int blockSubType) {
        setBlock((int) x, (int) y, (int) z, block, blockSubType);
    }

    public static void generate() {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float baseCellularNoise = (Noises.COHERERENT_NOISE.sample(x, z)+0.5f)/2;
                int surface = (int)(32*baseCellularNoise)+52;
                for (int y = surface; y >= 0; y--) {
                    setBlock(x, y, z, 2, 0);
                }
                if (surface < 63) {
                    setBlock(x, 63, z, 1, 13);
                    for (int y = 62; y >= surface; y--) {
                        setBlock(x, y, z, 1, 15);
                    }
                }
            }
        }
    }
}
