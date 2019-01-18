package com.wurmonline.server.behaviours;

import com.wurmonline.server.items.Item;

import java.util.HashMap;
import java.util.Map;

public class Vehicles {

    private static Map<Item, Vehicle> vehicles = new HashMap<>();

    public static Vehicle getVehicle(Item item) {
        return vehicles.getOrDefault(item, null);
    }

    public static Vehicle createVehicle(Item item) {
        Vehicle newVehicle = new Vehicle(1232345);
        vehicles.put(item, newVehicle);
        return newVehicle;
    }

    public static void reset() {
        vehicles.clear();
    }
}
