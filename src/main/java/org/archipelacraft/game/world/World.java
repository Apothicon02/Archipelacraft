package org.archipelacraft.game.world;

import org.archipelacraft.Main;
import org.archipelacraft.engine.ArchipelacraftMath;
import org.archipelacraft.game.ScheduledTicker;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.LightBlockType;
import org.archipelacraft.game.noise.Noises;
import org.archipelacraft.game.rendering.Textures;
import org.archipelacraft.game.world.shapes.Blob;
import org.archipelacraft.game.world.trees.*;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.archipelacraft.engine.Utils.condensePos;
import static org.archipelacraft.engine.Utils.distance;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.glTexSubImage3D;
import static org.lwjgl.opengl.GL30.*;

public class World {
    public static int size = 1024;
    public static int halfSize = 1024/2;
    public static int height = 320;
    public static int seaLevel = 63;
    public static short[] blocks = new short[(World.size*World.size*World.height)*2];
    public static short[] blocksLOD = new short[(World.size*World.size*World.height)/4];
    public static short[] blocksLOD2 = new short[(World.size*World.size*World.height)/16];
    public static byte[] lights = new byte[(World.size*World.size*World.height)*4];
    public static short[] heightmap = new short[World.size*World.size];
    public static short[] surfaceHeightmap = new short[World.size*World.size];
    public static Random seededRand = new Random(35311350L);

    public static boolean inBounds(int x, int y, int z) {
        return (x >= 0 && x < size && y >= 0 && y < height && z >= 0 && z < size);
    }

    public static void setLight(int x, int y, int z, int r, int b, int g, int s) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z)*4;
            lights[pos] = (byte)r;
            lights[pos+1] = (byte)b;
            lights[pos+2] = (byte)g;
            lights[pos+3] = (byte)s;
            if (Main.player != null) {
                glBindTexture(GL_TEXTURE_3D, Textures.lights.id);
                glTexSubImage3D(GL_TEXTURE_3D, 0, x, y, z, 1, 1, 1, GL_RGBA, GL_BYTE, ByteBuffer.allocateDirect(4).put((byte)r).put((byte)b).put((byte)g).put((byte)s).flip());
            }
        }
    }
    public static void setLight(int x, int y, int z, Vector4i light) {
        setLight(x, y, z, light.x, light.y, light.z, light.w);
    }
    public static Vector4i getLight(int x, int y, int z, boolean returnNull) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z)*4;
            return new Vector4i(lights[pos], lights[pos+1], lights[pos+2], lights[pos+3]);
        }
        return returnNull ? null : new Vector4i(0);
    }
    public static Vector4i getLight(int x, int y, int z) {
        return getLight(x, y, z, true);
    }
    public static Vector4i getLight(Vector3i pos) {
        return getLight(pos.x, pos.y, pos.z, true);
    }
    public static int getCorner(int x, int y, int z) {
        return 0;
    }

    public static void setBlock(int x, int y, int z, int block, int blockSubType, boolean replace, boolean priority, int tickDelay, boolean silent) {
        Vector2i existing = getBlock(x, y, z);
        if (existing != null && (replace || existing.x() == 0)) {
            Vector3i pos = new Vector3i(x, y, z);
            Vector4i oldLight = getLight(pos);
            byte r = 0;
            byte g = 0;
            byte b = 0;
            Vector2i newBlock = new Vector2i(block, blockSubType);
            BlockType blockType = BlockTypes.blockTypeMap.get(block);
            boolean lightChanged = BlockTypes.blockTypeMap.get(existing.x) instanceof LightBlockType;
            if (blockType instanceof LightBlockType lType) {
                lightChanged = true;
                r = lType.lightBlockProperties().r;
                g = lType.lightBlockProperties().g;
                b = lType.lightBlockProperties().b;
            }
            BlockType oldBlockType = BlockTypes.blockTypeMap.get(existing.x);
            setBlock(x, y, z, block, blockSubType);
            if (tickDelay > 0) {
                ScheduledTicker.scheduleTick(Main.currentTick+tickDelay, pos, 0);
            }
            if (!lightChanged) {
                lightChanged = blockType.blocksLight(newBlock) != oldBlockType.blocksLight(newBlock);
            }
            if (lightChanged) {
                setLight(x, y, z, r, g, b, 0);
            }

            if (blockType.obstructingHeightmap(new Vector2i(block, blockSubType)) != oldBlockType.obstructingHeightmap(existing)) {
                updateHeightmap(x, z);
            }

            if (lightChanged) {
                LightHelper.recalculateLight(pos, org.joml.Math.max(oldLight.x, r), org.joml.Math.max(oldLight.y, g), org.joml.Math.max(oldLight.z, b), oldLight.w);
            }

            if (block == 0) {
                BlockTypes.blockTypeMap.get(existing.x).onPlace(pos, existing, silent);
            } else {
                BlockTypes.blockTypeMap.get(block).onPlace(pos, new Vector2i(block, blockSubType), silent);
            }
        }
    }

    public static void setBlock(int x, int y, int z, int block, int blockSubType) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z)*2;
            blocks[pos] = (short)(block);
            blocks[pos+1] = (short)(blockSubType);
            if (Main.player != null) {
                glBindTexture(GL_TEXTURE_3D, Textures.blocks.id);
                glTexSubImage3D(GL_TEXTURE_3D, 0, x, y, z, 1, 1, 1, GL_RGBA_INTEGER, GL_INT, new int[]{block, blockSubType, 0, 0});
                boolean clear = true;
                loop:
                for (int cX = (int) Math.floor(x/4f)*4; cX < (Math.floor(x/4f)*4)+4; cX++) {
                    for (int cY = (int) Math.floor(y/4f)*4; cY < (Math.floor(y/4f)*4)+4; cY++) {
                        for (int cZ = (int) Math.floor(z/4f)*4; cZ < (Math.floor(z/4f)*4)+4; cZ++) {
                            if (blocks[condensePos(cX, cY, cZ)*2] > 0) {
                                clear = false;
                                break loop;
                            }
                        }
                    }
                }
                glTexSubImage3D(GL_TEXTURE_3D, 2, x/4, y/4, z/4, 1, 1, 1, GL_RGBA_INTEGER, GL_INT, new int[]{clear ? 0 : 1, 0, 0, 0});
                blocksLOD[(((((z/4)*(World.height/4))+(y/4))*(World.size/4))+(x/4))] = (short)(clear ? 0 : 1);
                loop:
                for (int cX = (int) Math.floor(x/16f)*16; cX < (Math.floor(x/16f)*16)+16; cX++) {
                    for (int cY = (int) Math.floor(y/16f)*16; cY < (Math.floor(y/16f)*16)+16; cY++) {
                        for (int cZ = (int) Math.floor(z/16f)*16; cZ < (Math.floor(z/16f)*16)+16; cZ++) {
                            if (blocks[condensePos(cX, cY, cZ)*2] > 0) {
                                clear = false;
                                break loop;
                            }
                        }
                    }
                }
                glTexSubImage3D(GL_TEXTURE_3D, 4, x/16, y/16, z/16, 1, 1, 1, GL_RGBA_INTEGER, GL_INT, new int[]{clear ? 0 : 1, 0, 0, 0});
                blocksLOD2[(((((z/16)*(World.height/16))+(y/16))*(World.size/16))+(x/16))] = (short)(clear ? 0 : 1);
            } else if (block > 0) {
                blocksLOD[(((((z/4)*(World.height/4))+(y/4))*(World.size/4))+(x/4))] = (short)(block);
                blocksLOD2[(((((z/16)*(World.height/16))+(y/16))*(World.size/16))+(x/16))] = (short)(block);
            }
        }
    }
    public static void setBlock(float x, float y, float z, int block, int blockSubType) {
        setBlock((int) x, (int) y, (int) z, block, blockSubType);
    }

    public static Vector2i getBlock(int x, int y, int z) {
        if (inBounds(x, y, z)) {
            int pos = condensePos(x, y, z)*2;
            return new Vector2i(blocks[pos], blocks[pos+1]);
        } else {
            return null;
        }
    }
    public static Vector2i getBlock(Vector3i pos) {
        return getBlock(pos.x, pos.y, pos.z);
    }
    public static Vector2i getBlock(float x, float y, float z) {
        return getBlock((int) x, (int) y, (int) z);
    }
    public static Vector2i getBlockNotNull(float x, float y, float z) {
        Vector2i block = getBlock((int) x, (int) y, (int) z);
        return block == null ? new Vector2i(0) : block;
    }

    public static void generate() {
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                float basePerlinNoise = (Noises.COHERERENT_NOISE.sample(x, z)+0.5f)/2;
                float baseCellularNoise = Noises.CELLULAR_NOISE.sample(x, z)/2;
                float centDist = (float) (distance(x, z, size/2, size/2)/halfSize);
                float centDistExp = (Math.max(0.5f, centDist)-0.5f);
                centDistExp *= centDistExp;
                int surface = (int)(((200*(Math.max(0.1f, baseCellularNoise)*basePerlinNoise))+70)-(centDistExp*300));
                surface = Math.max(8, surface);
                heightmap[condensePos(x, z)] = (short)(surface);
                for (int y = surface; y >= 0; y--) {
                    setBlock(x, y, z, 3, 0);
                }
            }
        }

        surfaceHeightmap = Arrays.copyOf(heightmap, heightmap.length);

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int maxSteepness = 0;
                int minNeighborY = height - 1;
                int condensedPos = condensePos(x, z);
                int surface = heightmap[condensedPos];
                for (int pos : new int[]{condensePos(Math.min(size - 1, x + 3), z), condensePos(Math.max(0, x - 3), z), condensePos(x, Math.min(size - 1, z + 3)), condensePos(x, Math.max(0, z - 3)),
                        condensePos(Math.max(0, x - 3), Math.max(0, z - 3)), condensePos(Math.min(size - 1, x + 3), Math.max(0, z - 3)), condensePos(Math.max(0, x - 3), Math.min(size - 1, z + 3)), condensePos(Math.min(size - 1, x + 3), Math.min(size - 1, z + 3))}) {
                    int nY = heightmap[pos];
                    minNeighborY = Math.min(minNeighborY, nY);
                    int steepness = Math.abs(surface - nY);
                    maxSteepness = Math.max(maxSteepness, steepness);
                }
                boolean flat = maxSteepness < 3;
                if (flat) {
                    if (surface < seaLevel) {
                        setBlock(x, seaLevel, z, 1, 13);
                        for (int y = 62; y > surface; y--) {
                            setBlock(x, y, z, 1, 15);
                        }
                    } else if (surface < seaLevel+3) {
                        setBlock(x, surface, z, BlockTypes.getId(BlockTypes.SAND), 0);
                        for (int newY = surface-1; newY >= surface-5; newY--) {
                            setBlock(x, newY, z, BlockTypes.getId(BlockTypes.SANDSTONE), 0);
                        }
                    } else {
                        setBlock(x, surface, z, 2, 0);
                        double flowerChance = seededRand.nextDouble();
                        setBlock(x, surface+1, z, 4 + (flowerChance > 0.95f ? (flowerChance > 0.97f ? 14 : 1) : 0), seededRand.nextInt(0, 3));
                    }
                } else {
                    if (surface < seaLevel) {
                        setBlock(x, seaLevel, z, 1, 13);
                        for (int y = 62; y > surface; y--) {
                            setBlock(x, y, z, 1, 15);
                        }
                    }
                    for (int newY = surface; newY >= surface-5; newY--) {
                        setBlock(x, newY, z, 55, 0);
                    }
                }
            }
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int surface = heightmap[(x*size)+z];
                Vector2i blockOn = getBlock(x, surface, z);
                float basePerlinNoise = Noises.COHERERENT_NOISE.sample(x, z);
                float randomNumber = seededRand.nextFloat();
                if (blockOn.x == 2) {
                    float foliageChanceExp = basePerlinNoise * basePerlinNoise;
                    if (randomNumber*10 < foliageChanceExp - 0.2f || randomNumber < 0.0002f) { //tree
                        float foliageType = seededRand.nextFloat();
                        if (foliageType < 0.0015f) { //1.5% chance the tree is dead
                            int maxHeight = seededRand.nextInt(6) + 12;
                            DeadOakTree.generate(blockOn, x, surface, z, maxHeight, 47, 0);
                            Blob.generate(blockOn, x, surface, z, 3, 0, (int) ((Math.random() + 1) * 3), new int[]{2, 23}, true);
                        } else if (foliageType < ArchipelacraftMath.gradient(surface, 82, 100, 1, 0)) {
                            if (randomNumber < 0.2f) { //80% chance to not generate anything
                                int maxHeight = seededRand.nextInt(19) + 5;
                                PineTree.generate(blockOn, x, surface, z, maxHeight, 35, 0, 36, 0);
                            }
                        } else {
                            int maxHeight = seededRand.nextInt(6) + 12;
                            int leavesHeight = seededRand.nextInt(3) + 3;
                            int radius = seededRand.nextInt(4) + 6;
                            OakTree.generate(blockOn, x, surface, z, maxHeight, radius, leavesHeight, 16, 0, 17, 0);
                        }
                    } else if ((randomNumber*10)+0.15f < basePerlinNoise-0.2f || randomNumber < 0.0005f) { //bush
                        int maxHeight = (int) (Math.random() + 1);
                        OakShrub.generate(blockOn, x, surface, z, maxHeight, 3 + (maxHeight * 2), 16, 0, 17, 0);
                    }
                } else if (blockOn.x == 23) {
                    int foliageChance = seededRand.nextInt(0, 400);
                    if (foliageChance == 0) { //tree
                        PalmTree.generate(blockOn, x, surface, z, seededRand.nextInt(8, 22), 25, 0, 27, 0);
                    } else if (randomNumber < 0.001f) {
                        setBlock(x, surface+1, z, BlockTypes.getId(BlockTypes.TORCH), 0);
                    }
                } else if (blockOn.x == 55) {
                    if (randomNumber < 0.08f) {
                        Blob.generate(blockOn, x, surface, z, randomNumber < 0.001f ? BlockTypes.getId(BlockTypes.KYANITE) : 8, 0, (int)(2 + (seededRand.nextFloat() * 8)));
                    }
                }
            }
        }

        for (int x = (size/2)-19; x <= size/2; x++) {
            for (int z = (size/2)-20; z < size/2; z++) {
                setBlock(x, 100, z, 15, 0);
                if (x == (size/2)-19 || x == (size/2) || z == (size/2)-20 || z == (size/2)-1) {
                    setBlock(x, 99, z, 15, 0);
                    setBlock(x, 98, z, 15, 0);
                    setBlock(x, 97, z, 15, 0);
                    setBlock(x, 96, z, 15, 0);
                    setBlock(x, 95, z, 15, 0);
                }
            }
        }
        setBlock(512, 95, 512, 15, 0);

//        for (int x = 0; x < size; x++) {
//            for (int y= 0; y < height; y++) {
//                //setLightNullable(x, y, (size/2)+2, new Vector4i(15, 15, 0, -1));
//                setLightNullable(x, y, (size/2)-2, new Vector4i(0, 0, 15, -1));
//            }
//        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                updateHeightmap(x, z);
            }
        }

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                for (int y = seaLevel; y <= heightmap[(x*size)+z]+1; y++) {
                    Vector2i thisBlock = getBlock(x, y, z);
                    if (BlockTypes.blockTypeMap.get(thisBlock.x) instanceof LightBlockType ||
                            getLight(x, y, z + 1, false).w() > 0 || getLight(x + 1, y, z, false).w() > 0 || getLight(x, y, z - 1, false).w() > 0 ||
                            getLight(x - 1, y, z, false).w() > 0 || getLight(x, y + 1, z, false).w() > 0 || getLight(x, y - 1, z, false).w() > 0) {
                        LightHelper.updateLight(new Vector3i(x, y, z), getBlock(x, y, z), getLight(x, y, z));
                    }
                }
            }
        }
    }

    public static void updateHeightmap(int x, int z) {
        boolean setHeightmap = false;
        for (int y = height-1; y >= 0; y--) {
            Vector2i block = getBlock(x, y, z);
            BlockType blockType = BlockTypes.blockTypeMap.get(block.x);
            Vector3i rgb = new Vector3i(0);
            if (blockType instanceof LightBlockType lBlock) {
                rgb.x = lBlock.lightBlockProperties().r;
                rgb.y = lBlock.lightBlockProperties().g;
                rgb.z = lBlock.lightBlockProperties().b;
            }
            if (!setHeightmap) {
                if (!blockType.obstructingHeightmap(block)) {
                    setLight(x, y, z, new Vector4i(rgb.x, rgb.y, rgb.z, 15));
                } else {
                    setHeightmap = true;
                    heightmap[condensePos(x, z)] = (short) (y);
                }
            } else {
                setLight(x, y, z, new Vector4i(rgb.x, rgb.y, rgb.z, 0));
            }
        }
    }
}
