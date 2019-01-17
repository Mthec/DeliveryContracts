package com.wurmonline.server.creatures;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.VolaTile;

public class Creature {
    private Communicator communicator;
    private Item inventory;
    private int villageId;
    private long id;
    private int carry = 100;
    public VolaTile currentTile = null;
    private static long nextWurmId = 50;
    private Item draggedItem = null;

    public long bridgeId = -10;

    public Creature() {
        communicator = new Communicator();
        inventory = new Item(ItemList.inventory);
        id = nextWurmId++;
    }

    public Communicator getCommunicator() {
        return communicator;
    }

    public Item getInventory() {
        return inventory;
    }

    public int getVillageId() {
        return villageId;
    }

    public void setVillageId(int villageId) {
        this.villageId = villageId;
    }

    public long getWurmId() {
        return id;
    }

    public long getBridgeId() {
        return bridgeId;
    }

    public boolean canCarry(int weight) {
        return weight <= carry;
    }

    public void setCarry(int weight) {
        carry = weight;
    }

    public int getFloorLevel(boolean b) {
        return 1;
    }

    public static Item createItem(int templateId, float ql) {
        Item newItem = new Item(templateId);
        newItem.setQualityLevel(ql);
        return newItem;
    }

    public boolean isWithinDistanceTo(float x, float y, float z, float maxDistance) {
        return true;
    }

    public VolaTile getCurrentTile() {
        return currentTile;
    }

    public Item getDraggedItem() {
        return draggedItem;
    }
}
