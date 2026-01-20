package org.archipelacraft.game.blocks.drops;

import kotlin.Pair;
import org.archipelacraft.game.blocks.types.BlockType;
import org.archipelacraft.game.blocks.types.BlockTypes;
import org.archipelacraft.game.items.Item;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.Map;

public class BlockDrops {
    public static Map<BlockType, Map<Pair<Float, Integer>[], Item>> blockTypeDropTable = Map.ofEntries(
            Map.entry(BlockTypes.STONE, LootTables.STONE),
            Map.entry(BlockTypes.OAK_LOG, LootTables.OAK_LOG),
            Map.entry(BlockTypes.SPRUCE_LEAVES, LootTables.LEAVES), Map.entry(BlockTypes.REDWOOD_LEAVES, LootTables.LEAVES),
            Map.entry(BlockTypes.OAK_LEAVES, LootTables.APPLE_LEAVES), Map.entry(BlockTypes.BIRCH_LEAVES, LootTables.APPLE_LEAVES), Map.entry(BlockTypes.WILLOW_LEAVES, LootTables.APPLE_LEAVES),
            Map.entry(BlockTypes.PALM_LEAVES, LootTables.ORANGE_LEAVES), Map.entry(BlockTypes.MAHOGANY_LEAVES, LootTables.ORANGE_LEAVES), Map.entry(BlockTypes.ACACIA_LEAVES, LootTables.ORANGE_LEAVES),
            Map.entry(BlockTypes.CHERRY_LEAVES, LootTables.CHERRY_LEAVES)
    );

    public static ArrayList<Item> getDrops(Vector2i block) {
        ArrayList<Item> drops = new ArrayList<>();
        Map<Pair<Float, Integer>[], Item> items = blockTypeDropTable.get(BlockTypes.blockTypeMap.get(block.x));
        if (items != null) {
            for (Pair<Float, Integer>[] drop : items.keySet()) {
                for (Pair<Float, Integer> chance : drop) {
                    if (Math.random() < chance.component1()) {
                        drops.add(items.get(drop).clone());
                    }
                }
            }
        }
        return drops;
    }
}
