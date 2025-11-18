package org.archipelacraft.game.world.shapes;

import org.archipelacraft.Main;
import org.archipelacraft.game.ScheduledTicker;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.CloudBlockType;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import static org.archipelacraft.engine.Utils.condensePos;
import static org.archipelacraft.game.world.World.*;

public class Blob {
    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace, boolean update) {
        Vector2i block = new Vector2i(blockType, blockSubType);
        BlockType type = BlockTypes.blockTypeMap.get(blockType);
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                if (inBounds(lX, y, lZ)) {
                    int condensedPos = condensePos(lX, lZ);
                    int surfaceY = heightmap[condensedPos];
                    for (int lY = y - radius; lY <= y + radius; lY++) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
                            boolean canReplace = true;
                            if (replace.length > 0) {
                                canReplace = false;
                                int replacing = getBlock(lX, lY, lZ).x;
                                for (int replaceable : replace) {
                                    if (replaceable == replacing) {
                                        canReplace = true;
                                        break;
                                    }
                                }
                            }
                            if (canReplace) {
                                if (update) {
                                    setBlock(lX, lY, lZ, blockType, blockSubType);
                                    if (type instanceof CloudBlockType) {
                                        ScheduledTicker.scheduleTick(Main.currentTick + 200 + (int) (Math.random() * 1000), new Vector3i(lX, lY, lZ), 1);
                                    }
                                } else {
                                    setBlock(lX, lY, lZ, blockType, blockSubType);
                                }
                                if (type.obstructingHeightmap(block)) {
                                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                                    if (type.blockProperties.blocksLight) {
                                        for (int extraY = lY; extraY >= surfaceY; extraY--) {
                                            setLight(lX, extraY, lZ, new Vector4i(0, 0, 0, 0));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, int[] replace) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, replace, false);
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, boolean update) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, new int[0], update);
    }

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, new int[0], false);
    }
}
