package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class MethodsItems {
    public static boolean isStealing = false;
    public static boolean isLootable = true;

    public static boolean checkIfStealing(Item item, Creature creature, Action action) {
        return isStealing;
    }

    public static boolean isLootableBy(Creature creature, Item item) {
        return isLootable;
    }

    public static void reset() {
        isStealing = false;
        isLootable = true;
    }

    public static boolean stopDragging(Creature creature, Item item) {
        Items.stopDragging(item);

        return true;
    }
}
