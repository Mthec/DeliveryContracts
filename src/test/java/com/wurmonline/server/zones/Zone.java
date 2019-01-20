package com.wurmonline.server.zones;

import com.wurmonline.server.items.Item;

import java.util.HashSet;
import java.util.Set;

public class Zone {

    public Set<Item> items = new HashSet<>();

    public VolaTile getTileOrNull(int x, int y) {
        return new VolaTile(0);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }

    public void addItem(Item item) {
        items.add(item);
    }
}
