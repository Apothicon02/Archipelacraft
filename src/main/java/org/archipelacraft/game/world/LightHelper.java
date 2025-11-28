package org.archipelacraft.game.world;

import org.archipelacraft.engine.Utils;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.blocks.types.LightBlockType;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.ArrayList;

import static org.archipelacraft.game.world.World.*;

public class LightHelper {
    public static ArrayList<Vector3i> lightQueue = new ArrayList<>();

    public static boolean queueLightUpdate(Vector3i pos) {
        boolean exists = lightQueue.contains(pos);
        if (!exists) {
            lightQueue.add(pos);
            return true;
        }
        return false;
    }

    public static void iterateLightQueue() {
        while (!lightQueue.isEmpty()) {
            Vector3i pos = lightQueue.getFirst();
            lightQueue.removeFirst();
            updateLight(pos, getBlock(pos), getLight(pos));
        }
    }

    public static void updateLight(Vector3i pos, Vector2i block, Vector4i light, int stack) {
        stack++;
        BlockType blockType = BlockTypes.blockTypeMap.get(block.x);
        int corners = getCorner(pos.x, pos.y, pos.z);
        boolean isLight = blockType instanceof LightBlockType;
        if (!blocksLight(block, corners) || isLight) {
            int r = Math.max(light.x(), isLight ? ((LightBlockType) blockType).lightBlockProperties().r : 0);
            int g = Math.max(light.y(), isLight ? ((LightBlockType) blockType).lightBlockProperties().g : 0);
            int b = Math.max(light.z(), isLight ? ((LightBlockType) blockType).lightBlockProperties().b : 0);
            int s = (pos.y > World.heightmap[Utils.condensePos(pos.x, pos.z)] ? 15 : light.w);
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
            }) {
                Vector2i neighbor = World.getBlock(neighborPos);
                Vector4i neighborLight = World.getLight(neighborPos);
                if (neighborLight != null) {
                    BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.x);
                    boolean isNLight = neighborBlockType instanceof LightBlockType;
                    if (!blocksLight(neighbor, getCorner(neighborPos.x, neighborPos.y, neighborPos.z)) || isNLight) {
                        r = Math.max(r, Math.max(neighborLight.x(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().r : 0) - 1);
                        g = Math.max(g, Math.max(neighborLight.y(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().g : 0) - 1);
                        b = Math.max(b, Math.max(neighborLight.z(), isNLight ? ((LightBlockType) neighborBlockType).lightBlockProperties().b : 0) - 1);
                        s = Math.max(s, neighborLight.w() - 1);
                    }
                }
            }
            setLight(pos.x, pos.y, pos.z, new Vector4i(r, g, b, s));
            for (Vector3i neighborPos : new Vector3i[]{
                    new Vector3i(pos.x, pos.y, pos.z + 1), new Vector3i(pos.x + 1, pos.y, pos.z), new Vector3i(pos.x, pos.y, pos.z - 1),
                    new Vector3i(pos.x - 1, pos.y, pos.z), new Vector3i(pos.x, pos.y + 1, pos.z), new Vector3i(pos.x, pos.y - 1, pos.z)
            }) {
                Vector4i nLight = getLight(neighborPos);
                if (nLight != null) {
                    if (isDarker(r, g, b, s, nLight)) {
                        if (stack < 10000) {
                            updateLight(neighborPos, World.getBlock(neighborPos), nLight, stack);
                        }
                    }
                }
            }
        }
    }
    public static void updateLight(Vector3i pos, Vector2i block, Vector4i light) {
        updateLight(pos, block, light, 0);
    }
    public static boolean isDarker(int r, int g, int b, int s, Vector4i darker) {
        return r-2 > darker.x() || g-2 > darker.y() || b-2 > darker.z() || s-2 > darker.w();
    }
    public static boolean blocksLight(Vector2i block, int corners) {
        if (!BlockTypes.blockTypeMap.get(block.x).blocksLight(block)) {
            return false;
        } else {
            int blocked = 0;
            for (int x = 0; x <= 1; x++) {
                for (int z = 0; z <= 2; z+=2) {
                    for (int y = 0; y <= 4; y+=4) {
                        int cornerIndex = y + z + x;
                        int temp = corners;
                        temp &= (~(1 << (cornerIndex - 1)));
                        if (temp == corners) {
                            blocked++;
                        }
                    }
                }
            }
            if (blocked >= 8) {
                return true;
            } else {
                return false;
            }
        }
    }

    public static void recalculateLight(Vector3i pos, int r, int g, int b, int s) {
        for (Vector3i neighborPos : new Vector3i[]{
                new Vector3i(pos.x, pos.y, pos.z + 1),
                new Vector3i(pos.x + 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y, pos.z - 1),
                new Vector3i(pos.x - 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y + 1, pos.z),
                new Vector3i(pos.x, pos.y - 1, pos.z)
        }) {
            Vector2i neighbor = World.getBlock(neighborPos);
            if (neighbor != null) {
                BlockType neighborBlockType = BlockTypes.blockTypeMap.get(neighbor.x);
                if (!blocksLight(neighbor, getCorner(neighborPos.x, neighborPos.y, neighborPos.z)) || neighborBlockType instanceof LightBlockType) {
                    Vector4i neighborLight = World.getLight(neighborPos);
                    if (neighborLight != null) {
                        if ((neighborLight.x() > 0 && neighborLight.x() < r) || (neighborLight.y() > 0 && neighborLight.y() < g) || (neighborLight.z() > 0 && neighborLight.z() < b) || (neighborLight.w() > 0 && neighborLight.w() < s)) {
                            byte nr = 0;
                            byte ng = 0;
                            byte nb = 0;
                            if (neighborBlockType instanceof LightBlockType lBlock) {
                                nr = lBlock.lightBlockProperties().r;
                                ng = lBlock.lightBlockProperties().g;
                                nb = lBlock.lightBlockProperties().b;
                            }
                            World.setLight(neighborPos.x, neighborPos.y, neighborPos.z, nr, ng, nb, (byte) (neighborLight.w() == 20 ? 20 : 0));
                            recalculateLight(neighborPos, neighborLight.x(), neighborLight.y(), neighborLight.z(), neighborLight.w());
                        }
                        queueLightUpdate(pos);
                    }
                }
            }
        }
    }
}