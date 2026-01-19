package org.archipelacraft.game.gameplay;

import org.archipelacraft.game.items.Item;

public class Inventory {
    public boolean open = false;
    public Item[] items = new Item[9*4];

    public Item getItem(int x, int y) {
        return items[(y*9)+x];
    }
    public void setItem(int x, int y, Item item) {
        items[(y*9)+x] = item;
    }

    public void addToInventory(Item item) {

    }
}
