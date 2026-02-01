package org.archipelacraft.game.gameplay;

import org.archipelacraft.Main;
import org.archipelacraft.engine.Window;
import org.archipelacraft.game.items.Item;
import org.archipelacraft.game.items.ItemTypes;
import org.joml.Vector2i;

import java.util.ArrayList;

public class Inventory {
    public boolean open = false;
    public Item[] items = new Item[9*4];
    public Item cursorItem = null;
    public Vector2i selectedSlot = new Vector2i(0);
    public int prevRMBDeposit = -1;
    public int interactCD = 0;

    public void tick(Window window) {
        if (interactCD > 0) {
            interactCD--;
        }
        if (!Main.wasRMBDown) {
            prevRMBDeposit = -1;
        }
        if (interactCD <= 0) {
            int selSlotId = selectedSlot.x+(selectedSlot.y*9);
            Item selItem = getItem(selSlotId);
            if (cursorItem == null) {
                if (selItem != null) {
                    Item newSelItem = selItem.clone();
                    if (Main.isLMBClick) {
                        cursorItem = newSelItem.clone();
                        newSelItem = null;
                        interactCD = 5;
                    } else if (Main.isRMBClick) {
                        cursorItem = newSelItem.clone();
                        float splitAmt = cursorItem.amount / 2.f;
                        int existAmt = (int) Math.floor(splitAmt);
                        if (existAmt <= 0) {
                            newSelItem = null;
                        } else {
                            newSelItem.amount = existAmt;
                        }
                        cursorItem.amount = (int) Math.ceil(splitAmt);
                        interactCD = 5;
                    }
                    setItem(selSlotId, newSelItem);
                }
            } else if (Main.isLMBClick) {
                if (selItem != null) {
                    if (cursorItem.type != selItem.type) { //swap item with slot
                        Item oldCursorItem = cursorItem.clone();
                        cursorItem = selItem.clone();
                        setItem(selSlotId, oldCursorItem);
                    } else if (addToSlot(selSlotId, cursorItem, cursorItem.amount) == null) { //dump contents into slot
                        cursorItem = null;
                    }
                } else if (addToSlot(selSlotId, cursorItem, cursorItem.amount) == null) { //dump contents into slot
                    cursorItem = null;
                }
                interactCD = 5;
            }
        }
        if (cursorItem != null) {
            if (cursorItem == null || cursorItem.amount <= 0 || cursorItem.type == ItemTypes.AIR) {
                cursorItem = null;
            } else if (interactCD <= 0) {
                int slotId = selectedSlot.x+(selectedSlot.y*9);
                if (Main.wasLMBDown) { //split evenly across several slots
//                    if (addToSlot(slotId, cursorItem, cursorItem.amount, false) == null) {
//                        cursorItem = null;
//                    }
//                    interactCD = 20;
                } else if (Main.wasRMBDown && prevRMBDeposit != slotId) {
                    if (addToSlot(slotId, cursorItem, 1) == null) {
                        cursorItem = null;
                    }
                    interactCD = 20;
                }
            }
        }
        if (window.scroll.y > 0) {
            scrollUp();
        } else if (window.scroll.y < 0) {
            scrollDown();
        }
    }

    public void init() {
        setItem(0, 0, new Item().type(ItemTypes.STEEL_SCYTHE));
        setItem(1, 0, new Item().type(ItemTypes.STEEL_PICK));
        setItem(2, 0, new Item().type(ItemTypes.STEEL_HATCHET));
        setItem(3, 0, new Item().type(ItemTypes.STEEL_SPADE));
        setItem(4, 0, new Item().type(ItemTypes.STEEL_HOE));
        setItem(7, 0, new Item().type(ItemTypes.APPLE).amount(1));
        setItem(8, 0, new Item().type(ItemTypes.ORANGE).amount(2));
        setItem(8, 1, new Item().type(ItemTypes.ORANGE).amount(1));
        setItem(8, 2, new Item().type(ItemTypes.CHERRY).amount(2));
        setItem(3, 3, new Item().type(ItemTypes.GLASS).amount(37));
        setItem(3, 2, new Item().type(ItemTypes.GLASS).amount(1));
        setItem(2, 3, new Item().type(ItemTypes.STICK).amount(60));
        setItem(1, 3, new Item().type(ItemTypes.OAK_LOG).amount(54));
        setItem(0, 3, new Item().type(ItemTypes.STONE).amount(64));
        setItem(0, 2, new Item().type(ItemTypes.MARBLE).amount(64));
    }

    public Item getItem(int index) {
        return items[index];
    }
    public Item getItem(int x, int y) {
        return getItem((y*9)+x);
    }
    public Item getItem(Vector2i xy) {
        return getItem((xy.y*9)+xy.x);
    }
    public void setItem(int slotId, Item item) {
        Item existing = items[slotId];
        if (item != null) {
            if (existing == null || item.type != existing.type || item.amount != existing.amount) {
                item.playSound(Main.player.pos);
            }
        } else if (existing != null) {
            existing.playSound(Main.player.pos);
        }
        items[slotId] = item;
    }
    public void setItem(int x, int y, Item item) {
        setItem((y*9)+x, item);
    }

    public void addToInventory(ArrayList<Item> items) {
        if (items != null && !items.isEmpty()) {
            for (Item item : items) {
                addToInventory(item);
            }
        }
    }

    public Item addToInventory(Item item) {
        loop:
        for (int y = 3; y >= 0; y--) { //first try merging with existing stacks
            for (int x = 0; x < 9; x++) {
                int i = (y*9)+x;
                Item slotItem = getItem(i);
                if (slotItem != null && slotItem.type == item.type) {
                    if (addToSlot(i, item, item.amount) == null) {
                        item = null;
                        break loop;
                    }
                }
            }
        }
        if (item != null) {
            loop:
            for (int y = 3; y >= 0; y--) { //then try adding to an empty slot
                for (int x = 0; x < 9; x++) {
                    int i = (y*9)+x;
                    Item slotItem = getItem(i);
                    if (slotItem == null || slotItem.type == ItemTypes.AIR) {
                        setItem(i, item.clone());
                        break loop;
                    }
                }
            }
        }
        return item;
    }

    public Item addToSlot(int existingId, Item item, int amount) {
        Item existing = getItem(existingId);
        if (existing == null || existing.amount <= 0 || existing.type == ItemTypes.AIR) {
            existing = item.clone();
            existing.amount = amount;
            item.amount -= amount;
        } else if (existing.type == item.type && existing.amount < existing.type.maxStackSize) {
            existing = existing.clone();
            int space = Math.min(amount, Math.min(item.amount, existing.type.maxStackSize - existing.amount));
            if (space > 0) {
                existing.amount += space;
                item.amount -= space;
            }
        }
        if (item.amount <= 0) {
            item = null;
        }
        setItem(existingId, existing);
        return item;
    }

    public void scrollUp() {
        Item[] newItems = new Item[items.length];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int row = y+1;
                if (row >= 4) {
                    row = 0;
                }
                newItems[(y*9)+x] = getItem(x, row);
            }
        }
        items = newItems;
    }
    public void scrollDown() {
        Item[] newItems = new Item[items.length];
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int row = y-1;
                if (row < 0) {
                    row = 3;
                }
                newItems[(y*9)+x] = getItem(x, row);
            }
        }
        items = newItems;
    }
}
