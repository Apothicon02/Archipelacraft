package org.archipelacraft.game.world.cover;

import org.archipelacraft.game.world.World;
import org.joml.Vector2i;

public class Plains {

    public static void generate(Vector2i blockOn, int x, int y, int z) {
        if (blockOn.x == 2) {
            double flowerChance = Math.random();
            World.setBlock(x, y + 1, z, 4 + (flowerChance > 0.95f ? (flowerChance > 0.97f ? 14 : 1) : 0), (int) (Math.random() * 3)); //make it not replace
        }
    }
}
