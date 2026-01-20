package org.archipelacraft.game.items;

public class Item implements Cloneable {
    public int amount = 1;
    public ItemType type = ItemTypes.AIR;

    public Item type(ItemType type) {
        this.type = type;
        return this;
    }
    public Item amount(int amount) {
        this.amount = amount;
        return this;
    }

    @Override
    public Item clone() {
        try {
            return (Item) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
