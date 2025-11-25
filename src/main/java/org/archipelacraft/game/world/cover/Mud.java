package org.archipelacraft.game.world.cover;

import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector4i;

import static org.archipelacraft.engine.Utils.condensePos;
import static org.archipelacraft.game.world.World.*;

public class Mud {
    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius, boolean replace) {
        Vector2i block = new Vector2i(blockType, blockSubType);
        BlockType type = BlockTypes.blockTypeMap.get(blockType);
        for (int lX = x - radius; lX <= x + radius; lX++) {
            for (int lZ = z - radius; lZ <= z + radius; lZ++) {
                if (inBounds(lX, y, lZ)) {
                    int condensedPos = condensePos(lX, lZ);
                    int surfaceY = heightmap[condensedPos];
                    for (int lY = Math.max(World.seaLevel, y - radius); lY <= y + radius; lY++) {
                        int xDist = lX - x;
                        int yDist = lY - y;
                        int zDist = lZ - z;
                        int dist = xDist * xDist + zDist * zDist + yDist * yDist;
                        if (dist <= radius * 3) {
                            if (replace ? BlockTypes.blockTypeMap.get(getBlock(lX, lY, lZ).x).blockProperties.isSolid : true) {
                                World.setBlock(lX, lY, lZ, blockType, blockSubType);
                                if (type.obstructingHeightmap(block)) {
                                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], lY);
                                    if (type.blockProperties.blocksLight) {
                                        for (int extraY = lY; extraY >= surfaceY; extraY--) {
                                            World.setLight(lX, extraY, lZ, new Vector4i(0, 0, 0, 0));
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

    public static void generate(Vector2i blockOn, int x, int y, int z, int blockType, int blockSubType, int radius) {
        generate(blockOn, x, y, z, blockType, blockSubType, radius, false);
    }
}
