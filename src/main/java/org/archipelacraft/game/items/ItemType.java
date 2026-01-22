package org.archipelacraft.game.items;

import org.archipelacraft.game.audio.SFX;
import org.archipelacraft.game.audio.Sounds;
import org.joml.Vector2i;

public class ItemType {
    public String name;
    public int maxStackSize = 1;
    public Vector2i atlasOffset = null;
    public ItemSFX sound = new ItemSFX(new SFX[]{Sounds.CLOUD}, 0.2f, 1);

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
    public ItemType sfx(ItemSFX sfx) {
        sound = sfx;
        return this;
    }
}
