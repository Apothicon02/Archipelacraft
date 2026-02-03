package org.archipelacraft.game.items;

import org.joml.Vector2i;
import org.joml.Vector3f;

public class Item implements Cloneable {
    public int dataLength = 6; //excludes this int
    public ItemType type = ItemTypes.AIR;
    public Vector3f pos = new Vector3f();
    public int amount = 1;
    public float rot = 0.f;
    public float hover = 0.f;
    public boolean hoverMeridiem = false;
    public int timeExisted = 0;
    public long prevTickTime = 0;

    public static Item load(int[] data, int offset) {
        return new Item().type(ItemTypes.itemTypeMap.get(data[offset++])).moveTo(new Vector3f(data[offset++]/100f, data[offset++]/100f, data[offset++]/100f)).amount(data[offset++]).timeExisted(data[offset++]);
    }
    public int[] getData() {
        return new int[]{dataLength, ItemTypes.getId(type), (int)pos.x()*100, (int)pos.y()*100, (int)pos.z()*100, amount, timeExisted};
    }

    public void tick() {
        long time = System.currentTimeMillis();
        if (prevTickTime != 0) {
            long dif = time - prevTickTime;
            timeExisted += dif;
            rot += dif / 100f;
            if (rot >= 360) {
                rot = 0;
            }
            float hoverInc = (dif / 2500f) * Math.min(Math.max(0.01f, 0.1f - hover) * 10, Math.max(0.01f, 0.1f - Math.abs(hover - 0.1f)) * 10);
            if (hoverMeridiem) {
                hover += hoverInc;
                if (hover >= 0.1) {
                    hover = 0.1f;
                    hoverMeridiem = false;
                }
            } else {
                hover -= hoverInc;
                if (hover < 0.f) {
                    hover = 0.f;
                    hoverMeridiem = true;
                }
            }
        }
        prevTickTime = time;
    }
    public Item moveTo(Vector3f pos) {
        this.pos = new Vector3f(pos.x, pos.y, pos.z);
        return this;
    }
    public Item type(ItemType type) {
        this.type = type;
        return this;
    }
    public Item amount(int amount) {
        this.amount = amount;
        return this;
    }
    public Item timeExisted(int newTime) {
        this.timeExisted = newTime;
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

    public void playSound(Vector3f pos) {
        type.sound.placed(pos);
    }

    public Vector2i place() {
        return type.blockToPlace;
    }
}
