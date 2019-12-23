package com.wurmonline.server.creatures;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.Trade;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.VolaTile;

import javax.annotation.Nullable;
import java.util.logging.Logger;

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
    private Trade trade;
    public boolean player = false;
    private TradeHandler tradeHandler;
    public boolean onSurface;
    public boolean withinDistance = true;
    public long vehicle;
    public int power;

    public Creature() {
        id = nextWurmId++;
        communicator = new Communicator(this);
        inventory = new Item(ItemList.inventory);
        inventory.setOwnerId(id);
        Creatures.getInstance().addCreature(this);
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
        return getFloorLevel();
    }

    public int getFloorLevel() {
        return 1;
    }

    public static Item createItem(int templateId, float ql) {
        Item newItem = new Item(templateId);
        newItem.setQualityLevel(ql);
        return newItem;
    }

    public boolean isWithinDistanceTo(float x, float y, float z, float maxDistance) {
        return withinDistance;
    }

    public VolaTile getCurrentTile() {
        return currentTile;
    }

    public long getVehicle() {
        return vehicle;
    }

    public Item getDraggedItem() {
        return draggedItem;
    }

    public void setDraggedItem(@Nullable Item item) {
        draggedItem = item;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public void startTrading() {}
    public void stopTrading() {}

    public boolean isPlayer() {
        return player;
    }

    public void addItemsToTrade() {
        for (Item item : inventory.getItems())
            trade.getTradingWindow(1).addItem(item);
    }

    public TradeHandler getTradeHandler() {
        if (tradeHandler == null)
            tradeHandler = new TradeHandler(this, trade);
        return tradeHandler;
    }

    public String getName() {
        return "Creature_" + id;
    }

    public boolean hasLink() {
        return player;
    }

    public boolean isLogged() {
        return player;
    }

    public boolean isDead() {
        return false;
    }

    public boolean isNpcTrader() {
        return !player;
    }

    public int getNumberOfShopItems() {
        return inventory.getItemCount();
    }

    public Village getCitizenVillage() {
        return null;
    }

    public Logger getLogger() {
        return Logger.getLogger(Creature.class.getName());
    }

    public TilePos getTilePos() {
        return null;
    }

    public boolean isOnSurface() {
        return onSurface;
    }

    public int getPower() {
        return power;
    }

    public Shop getShop() {
        return Economy.getShopFor(this);
    }

    public Item getCarriedItem(int templateId) {
        for (Item item : getInventory().getItems()) {
            if (item.getTemplateId() == templateId)
                return item;
        }
        return null;
    }

    public boolean isSalesman() {
        return isNpcTrader();
    }
}
