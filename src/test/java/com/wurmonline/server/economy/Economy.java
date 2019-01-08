package com.wurmonline.server.economy;

import java.util.ArrayList;
import java.util.List;

public class Economy {
    private static List<Shop> shops;

    public static void reset() {
        shops = new ArrayList<>();
    }

    public static Shop[] getTraders() {
        return shops.toArray(new Shop[0]);
    }

    public static void addTrader(Shop shop) {
        shops.add(shop);
    }
}
