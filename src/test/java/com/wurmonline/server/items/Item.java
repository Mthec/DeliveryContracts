package com.wurmonline.server.items;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import mod.wurmunlimited.delivery.DeliveryContractsMod;

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
    public long lastOwner = -10;
    private boolean planted;
    private Item parent = null;
    private int weight = 0;
    private Set<Creature> watchers = new HashSet<>();
    public boolean mailed;
    private long bridgeId = -10;
    public TradingWindow tradeWindow;

    // For modification during testing.
    public boolean busy;
    public boolean noTake;
    public boolean draggable;
    public boolean coin;
    public boolean liquid;
    public boolean banked;
    public boolean sign;
    public boolean streetLamp;
    public boolean flag;
    public boolean hollow;
    public boolean isDragged;
    public long lock = -10;
    public boolean unique;
    public long price = 1;
    public long value = 1;
    public boolean fullPrice = false;
    public boolean noSellBack = false;
    public byte material = 0;

    public Item(int templateId) {
        this.templateId = templateId;
        id = nextId++;
        Items.addItems(this);
    }

    public static int[] getDropTile(Creature performer) {
        return new int[] { 1, 1 };
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public int getTemplateId() {
        return templateId;
    }

    public ItemTemplate getTemplate() {
        return new ItemTemplate(templateId, name);
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
        return parent != null ? parent.getWurmId() : -10;
    }

    public Item getParent() {
        return parent;
    }

    public Item getParentOrNull() {
        return parent;
    }

    public long getTopParent() {
        Item toReturn = getTopParentOrNull();
        if (toReturn == null)
            return id;
        return toReturn.getWurmId();
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
        item.ownerId = ownerId;
        for (Item i : item.getItems())
            i.setOwnerId(ownerId);
        return items.add(item);
    }

    private void removeItem(Item item) {
        items.remove(item);
    }

    public Set<Item> getItems() {
        return items;
    }

    public Item[] getItemsAsArray() {
        if (templateId == DeliveryContractsMod.getTemplateId() && isTraded())
            return new Item[0];
        return items.toArray(new Item[0]);
    }

    public Item[] getAllItems(boolean b) {
        if (templateId == DeliveryContractsMod.getTemplateId() && isTraded())
            return new Item[0];
        return getItemsAsArray();
    }

    public int getItemCount() {
        return items.size();
    }

    public void putItemInfrontof(Creature creature) {
        Item parent = getParentOrNull();
        if (parent != null) {
            parent.removeItem(this);
            this.parent = null;
        }
        inFrontOf = creature;
        creature.currentTile.addItem(this);
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
        for (Item item : items) {
            item.setOwnerId(ownerId);
        }
    }

    public long getOwnerId() {
        return ownerId;
    }

    public void setIsPlanted(boolean planted) {
        this.planted = planted;
    }

    public boolean isPlanted() {
        return planted;
    }

    public boolean isAbility() {
        return false;
    }

    public boolean isInventory() {
        return false;
    }

    public boolean isBodyPart() {
        return false;
    }

    public boolean isBeingWorkedOn() {
        return busy;
    }

    public boolean isCoin() {
        return coin;
    }

    public boolean isLiquid() {
        return liquid;
    }

    public boolean isFullprice() {
        return fullPrice || isCoin();
    }

    public boolean isMailed() {
        return mailed;
    }

    public boolean isBanked() {
        return banked;
    }

    public boolean isBulkItem() {
        return templateId == ItemList.bulkItem;
    }

    public boolean isBulkContainer() {
        return templateId == ItemList.bulkContainer;
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
        if (templateId == DeliveryContractsMod.getTemplateId())
            return 0;
        return weight + items.stream().mapToInt(Item::getWeightGrams).sum();
    }

    public Creature[] getWatchers() throws NoSuchCreatureException {
        if (watchers == null)
            throw new NoSuchCreatureException("Watchers is null.");
        return watchers.toArray(new Creature[0]);
    }

    public void addWatcher(long inventoryWindow, Creature creature) {
        watchers.add(creature);
    }

    public void removeWatcher(Creature creature) {
        watchers.remove(creature);
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isSign() {
        return sign;
    }

    public boolean isStreetLamp() {
        return streetLamp;
    }

    public boolean isFlag() {
        return flag;
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

    public boolean isEmpty(boolean b) {
        if (templateId == DeliveryContractsMod.getTemplateId())
            return true;
        return getItemCount() == 0;
    }

    public void clear() {
        items.clear();
    }

    public boolean contains(Item item) {
        return items.contains(item);
    }

    public boolean isHollow() {
        return hollow;
    }

    public TradingWindow getTradeWindow() {
        return tradeWindow;
    }

    public void setTradeWindow(TradingWindow window) {
        tradeWindow = window;
    }

    public boolean isViewableBy(Creature creature) {
        return true;
    }

    public boolean isArtifact() {
        return false;
    }

    public boolean isRoyal() {
        return false;
    }

    public boolean isVillageDeed() {
        return templateId == ItemList.settlementDeed;
    }

    public boolean isHomesteadDeed() {
        return false;
    }

    public boolean isLockable() {
        return false;
    }

    public boolean isSealedByPlayer() {
        return false;
    }

    public Item dropItem(long itemId, boolean b) {
        Item item = null;

        for (Item i : items) {
            if (i.getWurmId() == itemId) {
                item = i;
                break;
            }
        }

        if (item != null) {
            item.setOwnerId(-10L);
            removeItem(item);
            item.parent = null;
        }

        return item;
    }

    public byte getMaterial() {
        return material;
    }

    public boolean isPurchased() {
        return false;
    }

    public int getPrice() {
        return (int)price;
    }

    public int getValue() {
        return (int)value;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public long getLockId() {
        return lock;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isCombine() {
        return false;
    }

    public boolean isNoSellback() {
        return noSellBack;
    }

    public boolean isTraded() {
        return tradeWindow != null;
    }

    public byte getRarity() {
        return 0;
    }

    public void setMailed(boolean isMail) {
        mailed = isMail;
    }

    // TODO - Do I need to test these options as well?  Happens in Wurm code so should be okay without.
    public boolean isSurfaceOnly() {
        return false;
    }

    public boolean isNoDrop() {
        return false;
    }

    public boolean isComponentItem() {
        return false;
    }

    public boolean isDecoration() {
        return false;
    }

    public boolean isOutsideOnly() {
        return false;
    }
}
