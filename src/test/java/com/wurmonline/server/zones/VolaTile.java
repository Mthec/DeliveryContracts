package com.wurmonline.server.zones;

import com.wurmonline.server.structures.Structure;

public class VolaTile {

    private final int numberOfItems;

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
}
