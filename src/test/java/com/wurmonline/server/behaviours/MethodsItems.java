package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.Servers;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.zones.VolaTile;

public class MethodsItems {
    public static boolean isStealing = false;
    public static boolean isLootable = true;
    public static boolean mayUseInventory;

    public static boolean checkIfStealing(Item item, Creature creature, Action action) {
        return isStealing;
    }

    public static boolean isLootableBy(Creature creature, Item item) {
        return isLootable;
    }

    public static void reset() {
        isStealing = false;
        isLootable = true;
        mayUseInventory = false;
    }

    public static boolean stopDragging(Creature creature, Item item) {
        Items.stopDragging(item);

        return true;
    }

    public static boolean mayUseInventoryOfVehicle(Creature creature, Item item) {
        return mayUseInventory;
    }

    // TODO - Any better way to call this?  Actual method contains too many extra methods.
    public static String[] drop(Creature performer, Item target, boolean onGround) {
        String[] fail = new String[0];
        if (!target.isCoin() && (performer.getPower() == 0 || Servers.localServer.testServer)) {
            int[] tilecoords = Item.getDropTile(performer);
            VolaTile t = performer.currentTile; //Zones.getTileOrNull(tilecoords[0], tilecoords[1], performer.isOnSurface());
            if (t != null) {
                if (t.getNumberOfItems(t.getDropFloorLevel(performer.getFloorLevel())) > 99) {
                    performer.getCommunicator().sendNormalServerMessage("That place is too littered with items already.", (byte)3);
                    return fail;
                }

                if (target.isDecoration() && t.getStructure() != null && t.getNumberOfDecorations(t.getDropFloorLevel(performer.getFloorLevel())) > 14) {
                    performer.getCommunicator().sendNormalServerMessage("That place is too littered with decorations already.", (byte)3);
                    return fail;
                }

                if (target.isOutsideOnly() && t.getStructure() != null) {
                    performer.getCommunicator().sendNormalServerMessage("You cannot drop that inside.", (byte)3);
                    return fail;
                }
            }
        }

        if (performer.getCurrentTile().getNumberOfItems(performer.getFloorLevel()) > 120) {
            performer.getCommunicator().sendNormalServerMessage("This area is too littered with items already.", (byte)3);
            return fail;
        }

        target.putItemInfrontof(performer);
        return new String[]{"You drop ", target.getName(), ".", "."};
    }
}
