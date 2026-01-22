package org.archipelacraft.game.blocks.drops;

import kotlin.Pair;
import org.archipelacraft.game.items.Item;
import org.archipelacraft.game.items.ItemTypes;

import java.util.Map;

public class LootTables {
    public static Map<Pair<Float, Integer>[], Item>  //order of chance pairs matters, lower chances should be first
            STONE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.STONE)),
            MARBLE = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.MARBLE)),
            OAK_LOG = Map.of(new Pair[]{new Pair<>(1.f, 1)}, new Item().type(ItemTypes.OAK_LOG)),
            LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK)),
            APPLE_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK),
                    new Pair[]{new Pair<>(0.01f, 2), new Pair<>(0.04f, 1)}, new Item().type(ItemTypes.APPLE)),
            ORANGE_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK),
                    new Pair[]{new Pair<>(0.01f, 2), new Pair<>(0.04f, 1)}, new Item().type(ItemTypes.ORANGE)),
            CHERRY_LEAVES = Map.of(
                    new Pair[]{new Pair<>(0.05f, 2), new Pair<>(0.2f, 1)}, new Item().type(ItemTypes.STICK),
                    new Pair[]{new Pair<>(0.02f, 2), new Pair<>(0.06f, 1)}, new Item().type(ItemTypes.CHERRY));
}
