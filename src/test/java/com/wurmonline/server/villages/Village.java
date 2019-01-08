package com.wurmonline.server.villages;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.Zones;

public class Village {
    private static int nextVillageId = 100;
    private int id;
    private VillageRole villageRole;

    public Village() {
        id = nextVillageId++;
        villageRole = new VillageRole();
        Zones.setVillage(this);
    }

    public int getId() {
        return id;
    }

    public VillageRole getRoleFor(Creature creature) {
        return villageRole;
    }
}
