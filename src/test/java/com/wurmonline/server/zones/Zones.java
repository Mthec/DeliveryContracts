package com.wurmonline.server.zones;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.villages.Village;

import java.util.HashMap;
import java.util.Map;

public class Zones {
    private static Village village = null;
    private static Map<Float, Zone> zones = new HashMap<>();

    public static void setVillage(Village village) {
        Zones.village = village;
    }

    public static Village getVillage(TilePos tilePos, boolean isSurfaced) {
        return village;
    }

    public static Zone getZone(int x, int y, boolean isOnSurface) {
        float key = ((float)(x + y));
        if (!zones.containsKey(key))
            zones.put(key, new Zone());
        return zones.get(key);
    }

    public static void reset() {
        village = null;
        zones.clear();
    }

    public static VolaTile getTileOrNull(int tilecoord, int tilecoord1, boolean onSurface) {
        return new VolaTile(0);
    }
}
