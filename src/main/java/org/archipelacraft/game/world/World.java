package org.archipelacraft.game.world;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.archipelacraft.Main;
import org.archipelacraft.engine.Utils;
import org.archipelacraft.game.ScheduledTicker;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.LightBlockType;
import org.archipelacraft.game.items.Item;
import org.archipelacraft.game.rendering.Renderer;
import org.archipelacraft.game.rendering.Textures;
import org.archipelacraft.game.world.types.WorldType;
import org.archipelacraft.game.world.types.WorldTypes;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Random;

import static org.archipelacraft.engine.Utils.condensePos;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.glTexSubImage3D;
import static org.lwjgl.opengl.GL30.*;

public class World {
    public static WorldType worldType = WorldTypes.TEMPERATE;
    public static int size = 1024;
    public static int halfSize = 1024/2;
    public static int height = 320;
    public static int seaLevel = 63;
    public static ArrayList<Item> items = new ArrayList<>();
    public static short[] blocks = new short[(World.size*World.size*World.height)*2];
    public static short[] blocksLOD = new short[(World.size*World.size*World.height)/4];
    public static short[] blocksLOD2 = new short[(World.size*World.size*World.height)/16];
    public static byte[] lights = new byte[(World.size*World.size*World.height)*4];
    public static short[] heightmap = new short[World.size*World.size];
    public static Random seededRand = new Random(35311350L);

    public static void clearData() {
        items.clear();
        blocks = new short[(World.size*World.size*World.height)*2];
        blocksLOD = new short[(World.size*World.size*World.height)/4];
        blocksLOD2 = new short[(World.size*World.size*World.height)/16];
        lights = new byte[(World.size*World.size*World.height)*4];
        heightmap = new short[World.size*World.size];
    }

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

    public static void saveWorld(String path) throws IOException {
        new File(path).mkdirs();

        String globalDataPath = path+"global.data";
        FileOutputStream out = new FileOutputStream(globalDataPath);
        byte[] globalData = Utils.intArrayToByteArray(new int[]{(int)(Renderer.time*1000), (int)(Main.timePassed*1000), Main.meridiem});
        out.write(globalData);
        out.close();

        String heightmapDataPath = path+"heightmap.data";
        out = new FileOutputStream(heightmapDataPath);
        byte[] heightmapData = Utils.shortArrayToByteArray(heightmap);
        out.write(heightmapData);
        out.close();

        String blocksPath = path + "blocks.data";
        out = new FileOutputStream(blocksPath);
        byte[] blocksData = Utils.shortArrayToByteArray(blocks);
        out.write(blocksData);
        String blockLODsPath = path + "blocksLOD.data";
        out = new FileOutputStream(blockLODsPath);
        byte[] blocksLODData = Utils.shortArrayToByteArray(blocksLOD);
        out.write(blocksLODData);
        String blocksLOD2Path = path + "blocksLOD2.data";
        out = new FileOutputStream(blocksLOD2Path);
        byte[] blocksLOD2Data = Utils.shortArrayToByteArray(blocksLOD2);
        out.write(blocksLOD2Data);
        String lightsPath = path + "lights.data";
        out = new FileOutputStream(lightsPath);
        out.write(lights);

        String itemsPath = path + "items.data";
        out = new FileOutputStream(itemsPath);
        IntArrayList data = new IntArrayList();
        int i = 0;
        for (Item item : items) {
            int[] itemData = item.getData();
            data.addElements(i, itemData);
            i += itemData[0]+1;
        }
        out.write(Utils.intArrayToByteArray(data.toIntArray()));

        out.close();
    }

    public static void loadWorld(String path) throws IOException {
        int[] globalData = Utils.flipIntArray(Utils.byteArrayToIntArray(new FileInputStream(path+"global.data").readAllBytes()));
        Renderer.time = globalData[0]/1000f;
        Main.timePassed = globalData[1]/1000f;
        Main.meridiem = globalData[2];
        heightmap = Utils.byteArrayToShortArray(new FileInputStream(path+"heightmap.data").readAllBytes());
        blocks = Utils.byteArrayToShortArray(new FileInputStream(path+"blocks.data").readAllBytes());
        blocksLOD = Utils.byteArrayToShortArray(new FileInputStream(path+"blocksLOD.data").readAllBytes());
        blocksLOD2 = Utils.byteArrayToShortArray(new FileInputStream(path+"blocksLOD2.data").readAllBytes());
        lights = new FileInputStream(path+"lights.data").readAllBytes();
        if (Files.exists(Path.of(path + "items.data"))) {
            int[] itemsData = Utils.flipIntArray(Utils.byteArrayToIntArray(new FileInputStream(path + "items.data").readAllBytes()));
            for (int i = 0; i < itemsData.length; ) {
                int itemDataLength = itemsData[i++];
                if (itemDataLength > 0) {
                    items.add(Item.load(itemsData, i));
                    i += itemDataLength;
                }
            }
        }
    }
}
