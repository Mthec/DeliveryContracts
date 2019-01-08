package com.wurmonline.server.zones;

import com.wurmonline.math.TilePos;
import com.wurmonline.server.villages.Village;

public class Zones {
    private static Village village = null;

    public static void setVillage(Village village) {
        Zones.village = village;
    }

    public static Village getVillage(TilePos tilePos, boolean isSurfaced) {
        return village;
    }
}
