package com.wurmonline.server.economy;

import com.wurmonline.server.creatures.Creature;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Economy {
    private static Economy instance;
    private static Map<Creature, Shop> shops;
    private Shop kingsShop = mock(Shop.class);
    public Map<Integer, Integer> boughtByTraders = new HashMap<>();
    public Map<String, Long> itemsSoldByTraders = new HashMap<>();
    public Map<Integer, Integer> soldByTraders = new HashMap<>();

    public static void reset() {
        instance = null;
        shops = new HashMap<>();
    }

    public static Shop[] getTraders() {
        return shops.values().toArray(new Shop[0]);
    }

    public static void addTrader(Creature trader) {
        Shop newShop = mock(Shop.class);
        when(newShop.getWurmId()).thenReturn(trader.getWurmId());
        when(newShop.getLocalSupplyDemand()).thenReturn(mock(LocalSupplyDemand.class));
        shops.put(trader, newShop);
    }

    public static Economy getEconomy() {
        if (instance == null)
            instance = new Economy();
        return instance;
    }

    public static Shop getShopFor(Creature creature) {
        return shops.get(creature);
    }

    public Shop getShop(Creature creature) {
        return shops.get(creature);
    }

    public Shop getKingsShop() {
        return kingsShop;
    }

    public void addItemBoughtByTraders(int templateId) {
        if (boughtByTraders.containsKey(templateId))
            boughtByTraders.put(templateId, boughtByTraders.get(templateId) + 1);
        else
            boughtByTraders.put(templateId, 1);
    }

    public void addItemSoldByTraders(int templateId) {
        if (soldByTraders.containsKey(templateId))
            soldByTraders.put(templateId, soldByTraders.get(templateId) + 1);
        else
            soldByTraders.put(templateId, 1);
    }

    public void addItemSoldByTraders(String name, long value, String windowowner, String watcher, int templateId) {
        itemsSoldByTraders.put(name, value);
    }
}
