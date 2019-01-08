package com.wurmonline.server.behaviours;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class MethodsItems {
    public static boolean isStealing = false;

    public static boolean checkIfStealing(Item item, Creature creature, Action action) {
        return isStealing;
    }
}
