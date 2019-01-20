package com.wurmonline.server.economy;

import com.wurmonline.server.creatures.Creature;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Economy {
    private static Economy instance;
    private static Map<Creature, Shop> shops;

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
        shops.put(trader, newShop);
    }

    public static Economy getEconomy() {
        if (instance == null)
            instance = new Economy();
        return instance;
    }

    public Shop getShop(Creature creature) {
        return shops.get(creature);
    }
}
