package com.wurmonline.server.creatures;

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
    public boolean player = true;
    private TradeHandler tradeHandler;

    public Creature() {
        id = nextWurmId++;
        communicator = new Communicator();
        inventory = new Item(ItemList.inventory);
        inventory.setOwnerId(id);
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
}
