package org.archipelacraft.game.blocks.types;

import org.archipelacraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector3i;


public class PowderBlockType extends BlockType {

    @Override
    public void lostSupport(Vector3i blockPos, Vector2i block) {
        Vector3i pos = new Vector3i(blockPos);

        if (BlockTypes.blockTypeMap.get(World.getBlock(pos.x, pos.y-1, pos.z).x).blockProperties.isFluidReplaceable) {
            World.setBlock(pos.x, pos.y, pos.z, 0, 0, true, false, 2, true);
            if (BlockTypes.blockTypeMap.get(World.getBlock(pos.x, pos.y-2, pos.z).x).blockProperties.isFluidReplaceable) {
                World.setBlock(pos.x, pos.y-1, pos.z, block.x, Math.min(5, block.y+1), true, false, 2, false);
            } else {
                World.setBlock(pos.x, pos.y-1, pos.z, block.y >= 5 ? block.x+1 : block.x, 0, true, false, -1, false);
            }
        }
    }

    @Override
    public boolean obstructingHeightmap(Vector2i block) {
        return blockProperties.obstructsHeightmap && block.y == 0;
    }

    @Override
    public boolean blocksLight(Vector2i block) {
        return blockProperties.blocksLight && block.y == 0;
    }

    public PowderBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
