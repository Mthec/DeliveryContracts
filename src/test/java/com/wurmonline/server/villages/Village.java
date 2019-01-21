package com.wurmonline.server.villages;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.zones.Zones;

public class Village {
    private static int nextVillageId = 100;
    private int id;
    private VillageRole citizenRole;
    private VillageRole visitorRole;

    public Village() {
        id = nextVillageId++;
        citizenRole = new VillageRole(true);
        visitorRole = new VillageRole();
        Zones.setVillage(this);
    }

    public int getId() {
        return id;
    }

    public VillageRole getRoleFor(Creature creature) {
        if (creature.getVillageId() == id)
            return citizenRole;
        return visitorRole;
    }
}
