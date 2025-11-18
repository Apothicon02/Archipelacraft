package org.archipelacraft.game.world.trees;

import kotlin.Pair;
import org.archipelacraft.game.world.FeatureHelper;
import org.archipelacraft.game.world.World;
import org.archipelacraft.game.world.trees.trunks.TwistingTrunk;
import org.joml.Vector2i;
import org.joml.Vector3i;
import org.joml.Vector4i;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.archipelacraft.engine.Utils.condensePos;
import static org.archipelacraft.game.world.World.heightmap;
import static org.archipelacraft.game.world.World.setLight;

public class DeadOakTree {
    public static void generate(Vector2i blockOn, int x, int y, int z, int maxHeight, int logType, int logSubType) {
        if (blockOn.x == 2 || blockOn.x == 23) {
            Pair<Map<Vector3i, Vector2i>, Set<Vector3i>> generatedTrunk = TwistingTrunk.generateTrunk(x, y, z, maxHeight, logType, logSubType, true, 8);
            Map<Vector3i, Vector2i> blocks = new HashMap<>(generatedTrunk.getFirst());
            blocks.forEach((pos, block) -> {
                if (FeatureHelper.inBounds(pos)) {
                    World.setBlock(pos.x, pos.y, pos.z, block.x, block.y);
                    int condensedPos = condensePos(pos.x, pos.z);
                    int surfaceY = heightmap[condensedPos];
                    heightmap[condensedPos] = (short) Math.max(heightmap[condensedPos], pos.y - 1);
                    for (int extraY = pos.y - 1; extraY >= surfaceY; extraY--) {
                        setLight(pos.x, extraY, pos.z, new Vector4i(0, 0, 0, 0));
                    }
                }
            });
        }
    }
}
