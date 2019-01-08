package com.wurmonline.server.structures;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class Blocking {
    public static boolean blocked = false;

    public static BlockingResult getBlockerBetween(Creature creature, Item item, int num) {
        if (!blocked)
            return null;
        return new BlockingResult();
    }
}
