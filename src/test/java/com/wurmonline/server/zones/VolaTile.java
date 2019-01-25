package com.wurmonline.server.zones;

import com.wurmonline.server.items.Item;
import com.wurmonline.server.structures.Structure;

public class VolaTile {

    private int numberOfItems;
    private int numberOfDecorations;

    public VolaTile(int numberOfItems) {
        this.numberOfItems = numberOfItems;
    }

    public int getNumberOfItems(int floor) {
        return numberOfItems;
    }

    public int getDropFloorLevel(int level) {
        return level;
    }

    public Structure getStructure() {
        return null;
    }

    public int getNumberOfDecorations(int dropFloorLevel) {
        return numberOfDecorations;
    }

    public void addItem(Item item) {
        numberOfItems += 1;
    }
}
