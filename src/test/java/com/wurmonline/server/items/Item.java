package com.wurmonline.server.items;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;

import java.util.HashSet;
import java.util.Set;

public class Item {
    private int templateId;
    private long data = -1;
    private static long nextId = 50;
    private long id;
    private Creature inFrontOf;
    private Set<Item> items = new HashSet<>();
    private String name = "";
    private String description = "";
    private float ql = 1.0f;
    private boolean inTheVoid;
    private int villageId;
    private long ownerId = -10;
    private boolean planted;
    private Item parent = null;
    private int weight = 0;
    private Set<Creature> watchers;
    public boolean mailed;
    private long bridgeId = -10;

    public boolean busy;
    public boolean noTake;
    public boolean draggable;

    public Item(int templateId) {
        this.templateId = templateId;
        id = nextId++;
        Items.addItems(this);
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public int getTemplateId() {
        return templateId;
    }

    public ItemTemplate getTemplate() {
        return new ItemTemplate(templateId);
    }

    public void setData(long data) {
        this.data = data;
    }

    public long getData() {
        return data;
    }

    public long getWurmId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public boolean setDescription(String description) {
        this.description = description;
        return true;
    }

    public float getQualityLevel() {
        return ql;
    }

    public void setQualityLevel(float ql) {
        this.ql = ql;
    }

    public long getParentId() {
        return parent.getWurmId();
    }

    public Item getParent() {
        return parent;
    }

    public Item getParentOrNull() {
        return parent;
    }

    public long getTopParent() {
        Item toReturn = getTopParentOrNull();
        return toReturn != this ? toReturn.getWurmId() : -10;
    }

    public Item getTopParentOrNull() {
        Item toReturn = this;
        Item parent = getParentOrNull();
        while (parent != null) {
            toReturn = parent;
            parent = parent.getParentOrNull();
        }
        return toReturn != this ? toReturn : null;
    }

    public boolean insertItem(Item item) {
        return insertItem(item, false);
    }

    public boolean insertItem(Item item, boolean unconditionally) {
        if (item.parent != null)
            item.parent.removeItem(item);
        item.parent = this;
        return items.add(item);
    }

    private void removeItem(Item item) {
        items.remove(item);
    }

    public Set<Item> getItems() {
        return items;
    }

    public Item[] getItemsAsArray() {
        return items.toArray(new Item[0]);
    }

    public int getItemCount() {
        return items.size();
    }

    public void putItemInfrontof(Creature creature) {
        inFrontOf = creature;
    }

    public boolean isInFrontOf(Creature creature) {
        return inFrontOf == creature;
    }

    public void putInVoid() {
        inTheVoid = true;
    }

    public boolean inTheVoid() {
        return inTheVoid;
    }

    public int getData2() {
        return villageId;
    }

    public void setVillageTokenId(int villageId) {
        this.villageId = villageId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setPlanted(boolean planted) {
        this.planted = planted;
    }

    public boolean isPlanted() {
        return planted;
    }

    public boolean isInventory() {
        return false;
    }

    public boolean isBodyPart() {
        return false;
    }

    public boolean isBeingWorkedOn() {
        return false;
    }

    public boolean isCoin() {
        return false;
    }

    public boolean isLiquid() {
        return false;
    }

    public boolean isFullprice() {
        return false;
    }

    public boolean isMailed() {
        return false;
    }

    public boolean isBanked() {
        return false;
    }

    public boolean isBulkItem() {
        return false;
    }

    public boolean isBulkContainer() {
        return false;
    }

    public int getBulkNums() {
        return 0;
    }

    public boolean isNoTake() {
        return noTake;
    }

    public boolean canBeDropped(boolean tradeCheck) {
        return true;
    }

    public long getBridgeId() {
        return bridgeId;
    }

    public TilePos getTilePos() {
        return new TilePos();
    }

    public boolean isOnSurface() {
        return true;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getWeightGrams() {
        return weight;
    }

    public int getFullWeight() {
        return weight + items.stream().mapToInt(Item::getWeightGrams).sum();
    }

    public Creature[] getWatchers() throws NoSuchCreatureException {
        if (watchers == null)
            throw new NoSuchCreatureException("Watchers is null.");
        return watchers.toArray(new Creature[0]);
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isTent() {
        return templateId == ItemList.tent;
    }

    public boolean isVehicle() {
        return templateId == ItemList.cartLarge;
    }

    public float getPosX() {
        return 1;
    }

    public float getPosY() {
        return 1;
    }

    public float getPosZ() {
        return 1;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setOnBridge(long bridgeId) {
        this.bridgeId = bridgeId;
    }

    public void setLastMaintained(long time) {

    }
}
