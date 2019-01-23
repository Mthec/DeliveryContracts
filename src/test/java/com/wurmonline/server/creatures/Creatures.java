package com.wurmonline.server.creatures;

import java.util.HashMap;
import java.util.Map;

public class Creatures {
    private Map<Long, Creature> creatures = new HashMap<>();
    private static Creatures instance;

    public static Creatures getInstance() {
        if (instance == null)
            instance = new Creatures();
        return instance;
    }

    public void addCreature(Creature creature) {
        creatures.put(creature.getWurmId(), creature);
    }

    public Creature getCreatureOrNull(long id) {
        if (!creatures.containsKey(id))
            return null;
        return creatures.get(id);
    }

    public static void reset() {
        instance = null;
    }
}
