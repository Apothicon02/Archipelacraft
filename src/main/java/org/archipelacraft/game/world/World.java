package org.archipelacraft.game.world;

import org.archipelacraft.Main;
import org.archipelacraft.engine.Utils;
import org.archipelacraft.game.ScheduledTicker;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.LightBlockType;
import org.archipelacraft.game.noise.Noises;
import org.archipelacraft.game.world.trees.OakTree;
import org.archipelacraft.game.world.trees.PalmTree;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.Random;

public class World {
    public static int size = 1024;
    public static int height = 320;
    public static int seaLevel = 63;
    public static short[] blocks = new short[World.size*World.size*World.height*4];
    public static short[] heightmap = new short[World.size*World.size];
    public static Random seededRand = new Random();

    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < size && y >= 0 && y < height && z >= 0 && z < size);
    }

    public static void setLight(int x, int y, int z, Vector4i light) {

    }

    public static void setBlock(int x, int y, int z, int block, int blockSubType, boolean replace, boolean priority, int tickDelay, boolean silent) {
        if (inBounds(x, y, z)) {
            Vector2i existing = getBlock(x, y, z);
            if (replace || existing.x() == 0) {
                Vector3i pos = new Vector3i(x, y, z);
                Vector4i oldLight = new Vector4i(0);//getLight(pos);
                byte r = 0;
                byte g = 0;
                byte b = 0;
                BlockType blockType = BlockTypes.blockTypeMap.get(block);
                boolean lightChanged = false;
                if (blockType instanceof LightBlockType lType) {
                    lightChanged = true;
                    r = lType.lightBlockProperties().r;
                    g = lType.lightBlockProperties().g;
                    b = lType.lightBlockProperties().b;
                }
                Vector2i oldBlock = getBlock(x, y, z);
                BlockType oldBlockType = BlockTypes.blockTypeMap.get(oldBlock.x);
                setBlock(x, y, z, block, blockSubType);
                if (tickDelay > 0) {
                    ScheduledTicker.scheduleTick(Main.currentTick+tickDelay, pos, 0);
                }
                if (!lightChanged) {
                    lightChanged = blockType.blockProperties.blocksLight != oldBlockType.blockProperties.blocksLight;
                }
                if (lightChanged) {
                    //setLight(x, y, z, r, g, b, 0, pos);
                }

//                if (blockType.obstructingHeightmap(new Vector2i(block, blockSubType)) != oldBlockType.obstructingHeightmap(oldBlock)) {
//                    updateHeightmap(x, z, true);
//                }

                if (lightChanged) {
                    //recalculateLight(pos, org.joml.Math.max(oldLight.x, r), org.joml.Math.max(oldLight.y, g), org.joml.Math.max(oldLight.z, b), oldLight.w);
                }

                if (block == 0) {
                    BlockTypes.blockTypeMap.get(existing.x).onPlace(pos, existing, silent);
                } else {
                    BlockTypes.blockTypeMap.get(block).onPlace(pos, new Vector2i(block, blockSubType), silent);
                }
            }
        }
    }

    public static void setBlock(int x, int y, int z, int block, int blockSubType) {
        if (inBounds(x, y, z)) {
            int pos = Utils.condensePos(x, y, z);
            blocks[pos] = (short)(block);
            blocks[pos+1] = (short)(blockSubType);
        }
    }
    public static void setBlock(float x, float y, float z, int block, int blockSubType) {
        setBlock((int) x, (int) y, (int) z, block, blockSubType);
    }

    public static Vector2i getBlock(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            int pos = Utils.condensePos(x, y, z);
            return new Vector2i(blocks[pos], blocks[pos+1]);
        } else {
            return new Vector2i(0);
        }
    }
    public static Vector2i getBlock(Vector3i pos) {
        return getBlock(pos.x, pos.y, pos.z);
    }
    public static Vector2i getBlock(float x, float y, float z) {
        return getBlock((int) x, (int) y, (int) z);
    }

    public static void generate() {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float baseCellularNoise = (Noises.COHERERENT_NOISE.sample(x, z)+0.5f)/2;
                int surface = (int)(32*baseCellularNoise)+52;
                heightmap[(x*size)+z] = (short)(surface);
                for (int y = surface; y >= 0; y--) {
                    setBlock(x, y, z, 3, 0);
                }
                if (surface < 63) {
                    setBlock(x, 63, z, 1, 13);
                    for (int y = 62; y >= surface; y--) {
                        setBlock(x, y, z, 1, 15);
                    }
                } else if (surface == 63) {
                    setBlock(x, surface, z, 23, 0);
                } else {
                    setBlock(x, surface, z, 2, 0);
                    double flowerChance = seededRand.nextDouble();
                    setBlock(x, surface+1, z, 4 + (flowerChance > 0.95f ? (flowerChance > 0.97f ? 14 : 1) : 0), seededRand.nextInt(0, 3));
                }
            }
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int surface = heightmap[(x*size)+z];
                Vector2i blockOn = getBlock(x, surface, z);
                if (blockOn.x == 2) {
                    int foliageChance = seededRand.nextInt(0, 1000);
                    if (foliageChance == 0) { //tree
                        int maxHeight = (int) (Math.random() * 6) + 12;
                        int leavesHeight = (int) (Math.random() * 3) + 3;
                        int radius = (int) (Math.random() * 4) + 6;
                        OakTree.generate(blockOn, x, surface, z, maxHeight, radius, leavesHeight, 16, 0, 17, 0);
                    }
                } else if (blockOn.x == 23) {
                    int foliageChance = seededRand.nextInt(0, 1000);
                    if (foliageChance == 0) { //tree
                        PalmTree.generate(blockOn, x, surface, z, seededRand.nextInt(8, 22), 25, 0, 27, 0);
                    }
                }
            }
        }
    }
}
