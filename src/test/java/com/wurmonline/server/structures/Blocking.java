package com.wurmonline.server.structures;

import com.wurmonline.math.Vector3f;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

public class Blocking {
    public static boolean blocked = false;

    public static class FakeBlocker implements Blocker {

        @Override
        public String getName() {
            return "wall";
        }
    }

    public static BlockingResult getBlockerBetween(Creature creature, Item item, int num) {
        if (!blocked)
            return null;
        BlockingResult result = new BlockingResult();
        result.addBlocker(new FakeBlocker(), new Vector3f(1,1,1), 1);
        return result;
    }
}
