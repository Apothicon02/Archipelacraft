package org.archipelacraft.game.blocks.types;

import kotlin.Pair;
import org.archipelacraft.game.blocks.Tag;
import org.archipelacraft.game.blocks.Tags;
import org.archipelacraft.game.world.World;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static org.archipelacraft.game.world.World.getBlock;

public class PlantBlockType extends BlockType {

    @Override
    public void lostSupport(Vector3i pos, Vector2i block) {
        boolean onSediment = Tags.sediment.tagged.contains(getBlock(pos.x, pos.y-1, pos.z).x);
        World.setBlock(pos.x, pos.y, pos.z, onSediment ? BlockTypes.getId(BlockTypes.DEAD_BUSH) : 0, onSediment ? (int)(Math.random()*1.9) : 0, true, false, 2, false);
    }

    @Override
    public void onPlace(Vector3i pos, Vector2i block, boolean isSilent) {
        if (!isSilent) {
            blockProperties.blockSFX.placed(new Vector3f(pos.x, pos.y, pos.z));
        }
        Vector2i blockOn = getBlock(pos.x, pos.y-1, pos.z);
        boolean survives = false;
        for (Pair<Tag, Tag> pair : Tags.survivalTags) {
            if (pair.getFirst().tagged.contains(block.x)) {
                if (pair.getSecond().tagged.contains(blockOn.x)) {
                    survives = true;
                    break;
                }
            }
        }
        if (!survives) {
            lostSupport(pos, block);
        }
    }

    public PlantBlockType(BlockProperties blockProperties) {
        super(blockProperties);
    }
}
