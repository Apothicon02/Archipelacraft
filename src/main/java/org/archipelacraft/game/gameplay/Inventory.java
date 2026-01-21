package org.archipelacraft.game.gameplay;

import org.archipelacraft.Main;
import org.archipelacraft.engine.MouseInput;
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

    public void tick(MouseInput mouseInput) {
        if (interactCD > 0) {
            interactCD--;
        }
        if (!Main.wasRMBDown) {
            prevRMBDeposit = -1;
        }
        if (interactCD <= 0) {
            int selSlotId = selectedSlot.x+(selectedSlot.y*9);
            if (cursorItem == null) {
                if (Main.isLMBClick) {
                    cursorItem = items[selSlotId];
                    if (cursorItem != null) {
                        items[selSlotId] = null;
                        interactCD = 20;
                    }
                } else if (Main.isRMBClick) {
                    cursorItem = items[selSlotId];
                    if (cursorItem != null) {
                        float splitAmt = cursorItem.amount / 2.f;
                        int existAmt = (int) Math.floor(splitAmt);
                        if (existAmt <= 0) {
                            items[selSlotId] = null;
                        } else {
                            items[selSlotId].amount = existAmt;
                        }
                        cursorItem.amount = (int) Math.ceil(splitAmt);
                        interactCD = 20;
                    }
                }
            } else if (Main.isLMBClick && cursorItem.amount < cursorItem.type.maxStackSize && cursorItem.type == items[selSlotId].type) {
//                int space = Math.min(items[selSlotId].amount, cursorItem.type.maxStackSize - cursorItem.amount);
//                if (space > 0) {
//                    cursorItem.amount += space;
//                    items[selSlotId].amount -= space;
//                    if (items[selSlotId].amount <= 0) {
//                        items[selSlotId] = null;
//                    }
//                } fix it replacing at the same time
            }
        }
        if (cursorItem != null) {
            if (cursorItem == null || cursorItem.amount <= 0 || cursorItem.type == ItemTypes.AIR) {
                cursorItem = null;
                interactCD = 20;
            } else if (interactCD <= 0) {
                int slotId = selectedSlot.x+(selectedSlot.y*9);
                if (Main.wasLMBDown) {
                    if (addToSlot(slotId, cursorItem, cursorItem.amount, true) == null) {
                        cursorItem = null;
                    }
                    interactCD = 20;
                } else if (Main.wasRMBDown && prevRMBDeposit != slotId) {
                    if (addToSlot(slotId, cursorItem, 1, false) == null) {
                        cursorItem = null;
                    }
                    interactCD = 20;
                }
            }
        }
        if (mouseInput.scroll.y > 0) {
            scrollUp();
        } else if (mouseInput.scroll.y < 0) {
            scrollDown();
        }
    }

    public Item getItem(int index) {
        return items[index];
    }
    public Item getItem(int x, int y) {
        return items[(y*9)+x];
    }
    public void setItem(int x, int y, Item item) {
        items[(y*9)+x] = item;
    }

    public void addToInventory(ArrayList<Item> items) {
        if (items != null && !items.isEmpty()) {
            for (Item item : items) {
                addToInventory(item);
            }
        }
    }

    public void addToInventory(Item item) {
        for (int i = items.length - 1; i >= 0; i--) {
            if (addToSlot(i, item, item.amount, false) == null) {
                break;
            }
        }
    }

    public Item addToSlot(int existingId, Item item, int amount, boolean replace) {
        Item existing = items[existingId];
        boolean changedAnything = false;
        if (existing == null || existing.amount <= 0 || existing.type == ItemTypes.AIR) {
            existing = item.clone();
            existing.amount = amount;
            item.amount -= amount;
            changedAnything = true;
        } else if (existing.type == item.type && existing.amount < existing.type.maxStackSize) {
            int space = Math.min(amount, Math.min(item.amount, existing.type.maxStackSize - existing.amount));
            if (space > 0) {
                existing.amount += space;
                item.amount -= space;
                changedAnything = true;
            }
        }
        if (item.amount <= 0) {
            item = null;
            changedAnything = true;
        }
        if (replace && !changedAnything) {
            Item tempExisting = existing.clone();
            existing = item.clone();
            item = tempExisting;
        }
        items[existingId] = existing;
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
                newItems[(y*9)+x] = items[(row*9)+x];
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
                newItems[(y*9)+x] = items[(row*9)+x];
            }
        }
        items = newItems;
    }
}
