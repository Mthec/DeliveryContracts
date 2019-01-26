package com.wurmonline.server.creatures;


import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.items.Item;

public class Communicator {
    private final Creature creature;
    private String lastMessage = "";

    public Communicator(Creature creature) {
        this.creature = creature;
    }

    public void sendNormalServerMessage(String message, byte b) {
        sendNormalServerMessage(message);
    }

    public void sendNormalServerMessage(String message) {
        System.out.println(message);
        lastMessage = message;
    }

    public void sendSafeServerMessage(String message) {
        System.out.println(message);
        lastMessage = message;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void sendStartTrading(Creature creature) {}
    public void sendAddToInventory(Item item, long l1, long l2, int i1) {}
    public void sendUpdateInventoryItem(Item item) {}
    public void sendRemoveFromInventory(Item item, long l1) {}
    public void sendTradeAgree(Creature creature, boolean b) {}
    public void sendCloseTradeWindow() {}
    public void sendTradeChanged(int i) {}

    public boolean sendCloseInventoryWindow(long wurmId) {
        try {
            Item item = Items.getItem(wurmId);
            item.removeWatcher(creature);
        } catch (NoSuchItemException e) {
            e.printStackTrace();
        }
        return true;
    }
}
