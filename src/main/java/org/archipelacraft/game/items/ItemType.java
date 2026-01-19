package org.archipelacraft.game.items;

import org.joml.Vector2i;

public class ItemType {
    public String name;
    public int maxStackSize = 1;
    public Vector2i atlasOffset = null;

    public ItemType(String name) {
        this.name = name;
    }

    public ItemType maxStackSize(int size) {
        maxStackSize = size;
        return this;
    }
    public ItemType atlasOffset(int x, int y) {
        atlasOffset = new Vector2i(x, y);
        return this;
    }
}
