package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.WurmId;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.Blocking;
import com.wurmonline.server.structures.BlockingResult;
import com.wurmonline.server.structures.Structure;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.VillageRole;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.util.MaterialUtilities;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import mod.wurmunlimited.delivery.PackResult;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PackContractAction implements ModAction, BehaviourProvider, ActionPerformer {
    private static final Logger logger = Logger.getLogger(PackContractAction.class.getName());
    private final short actionId;
    private final ActionEntry actionEntry;

    public PackContractAction() {
        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "Pack up", "packing",
                new int[] {
                      ActionTypes.ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM,
                      ActionTypes.ACTION_TYPE_QUICK,
                      ActionTypes.ACTION_TYPE_NONSTACKABLE
                }).range(2).build();

        ModActions.registerAction(actionEntry);
    }

    // For testing.
    PackContractAction(short actionId) {
        this.actionId = actionId;
        actionEntry = null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        if (subject != null && target != null) {
            if (subject.getTemplateId() == DeliveryContractsMod.getTemplateId() && subject.getItemCount() == 0 && checkTake(performer, target).wasSuccessful()) {
                return Collections.singletonList(actionEntry);
            }
        }
        return null;
    }

    private PackResult checkTake(Creature performer, Item target) {
        if (target.isBeingWorkedOn()) {
            return PackResult.TARGET_IN_USE(target.getName());
        } else {
            long ownId = target.getOwnerId();
            if (ownId != -10L && ownId != performer.getWurmId()) {
                return PackResult.TARGET_HAS_DIFFERENT_OWNER(target.getName());
            }

            if (target.isCoin()) {
                return PackResult.TARGET_IS_COIN();
            }

            if (target.isBodyPart()) {
                return PackResult.YOU_CANNOT_FIT();
            }

            if (target.isMailed() || target.isBanked() || target.isInventory()) {
                return PackResult.TARGET_IS_UNREACHABLE();
            }

            if (target.isLiquid()) {
                return PackResult.TARGET_IS_LIQUID();
            }

            if ((target.isBulkContainer() || target.isTent()) && !target.isEmpty(true)) {
                return PackResult.TARGET_FILLED_BULK_CONTAINER();
            }

            if (target.isBulkItem()) {
                return PackResult.TARGET_BULK_ITEM();
            }

            if (!target.canBeDropped(true) || target.isFullprice()) {
                return PackResult.TARGET_CANNOT_BE_DROPPED(target.getName());
            }

            if (target.isTent()) {
                Vehicle vehicle = Vehicles.getVehicle(target);
                if (vehicle != null && vehicle.getDraggers() != null && vehicle.getDraggers().size() > 0) {
                    return PackResult.HITCHED();
                }
            }

            try {
                BlockingResult result = Blocking.getBlockerBetween(performer, target, 4);
                if (result != null) {
                    return PackResult.TARGET_BLOCKED(target.getName(), result.getFirstBlocker().getName());
                }

                if (target.isUnique()) {
                    return PackResult.TARGET_IS_UNIQUE(target.getName());
                } else {
                    boolean sameVehicle = false;
                    Item top = target.getTopParentOrNull();
                    if (top != null && top.isVehicle() && top.getWurmId() == performer.getVehicle()) {
                        sameVehicle = true;
                    }

                    if (!sameVehicle && !performer.isWithinDistanceTo(target.getPosX(), target.getPosY(), target.getPosZ(), 5.0F)) {
                        return PackResult.TOO_FAR_AWAY(target.getName());
                    }

                    Zone zone = Zones.getZone((int)target.getPosX() >> 2, (int)target.getPosY() >> 2, target.isOnSurface());
                    VolaTile tile = zone.getTileOrNull((int)target.getPosX() >> 2, (int)target.getPosY() >> 2);
                    if (tile != null) {
                        Structure struct = tile.getStructure();
                        VolaTile tile2 = performer.getCurrentTile();
                        if (tile2 != null) {
                            if (tile.getStructure() != struct && (struct == null || !struct.isTypeBridge())) {
                                return PackResult.TARGET_BLOCKED(target.getName(), "wall");
                            }
                        } else if (struct != null && !struct.isTypeBridge()) {
                            return PackResult.TARGET_BLOCKED(target.getName(), "wall");
                        }
                    }

                    if (ownId != performer.getWurmId()) {
                        Village village = Zones.getVillage(target.getTilePos(), target.isOnSurface());
                        if (village != null) {
                            VillageRole roles = village.getRoleFor(performer);
                            if (roles != null) {
                                if ((target.isPlanted() && !roles.mayPickupPlanted()) || !roles.mayPickup())
                                    return PackResult.INSUFFICIENT_VILLAGE_PERMISSIONS();
                            }
                        }
                    }

                    long topParentId = target.getTopParent();
                    if (!MethodsItems.isLootableBy(performer, target)) {
                        return PackResult.MAY_NOT_LOOT_THAT_ITEM();
                    }

                    boolean mayUseVehicle = true;

                    Item topParent;
                    try {
                        topParent = Items.getItem(topParentId);
                        if (topParent.isDraggable()) {
                            mayUseVehicle = MethodsItems.mayUseInventoryOfVehicle(performer, topParent);
                        }

                        if (!mayUseVehicle && target.lastOwner != performer.getWurmId() && (topParent.isVehicle() && topParent.getLockId() != -10L || Items.isItemDragged(topParent)) && performer.getDraggedItem() != topParent) {
                            return PackResult.VEHICLE_IS_WATCHED(topParent.getName());
                        }
                    } catch (NoSuchItemException ignored) {}

                    if (MethodsItems.checkIfStealing(target, performer, null)) {
                        return PackResult.NEEDS_TO_STEAL(target.getName());
                    }

                    if (target.isPlanted() && !ItemBehaviour.isSignManipulationOk(target, performer, Actions.TAKE))
                        return PackResult.TARGET_PLANTED_BY_OTHER();

                    return PackResult.SUCCESS();
                }
            } catch (NoSuchZoneException var22) {
                logger.log(Level.WARNING, var22.getMessage(), var22);
            }
        }
        return PackResult.UNKNOWN_FAILURE();
    }

    private void actuallyTake(Creature performer, Item target, Item contract) throws NoSuchItemException, NoSuchZoneException {
        if (target.getTopParent() == target.getWurmId()) {
            try {
                for (Creature watcher : target.getWatchers()) {
                    watcher.getCommunicator().sendCloseInventoryWindow(target.getWurmId());
                }
            } catch (NoSuchCreatureException ignored) {}
        }

        if (WurmId.getType(target.getParentId()) == 6) {
            Item i = Items.getItem(target.getParentId());
            i.dropItem(target.getWurmId(), true);
        }

        int x = (int)target.getPosX() >> 2;
        int y = (int)target.getPosY() >> 2;
        Zone zone = Zones.getZone(x, y, target.isOnSurface());
        zone.removeItem(target);
        if (performer.getDraggedItem() == target) {
            MethodsItems.stopDragging(performer, target);
        }

        if (target.isPlanted()) {
            target.setIsPlanted(false);
            if (target.isAbility()) {
                target.hatching = false;
                target.setRarity((byte)0);
            }
        }

        if (target.getTemplate().isContainerWithSubItems()) {
            ArrayList<Item> toMove = new ArrayList<>();
            for (Item item : target.getItems()) {
                if (item.isPlacedOnParent()) {
                    toMove.add(item);
                }
            }

            for (Item item : toMove) {
                target.dropItem(item.getWurmId(), true);
                Zones.getZone(item.getTileX(), item.getTileY(), target.isOnSurface()).addItem(item);
                performer.getCommunicator().sendNormalServerMessage("The " + item.getName() + " drops to the ground.");
            }
        }

        contract.insertItem(target, true);
        performer.getCommunicator().sendUpdateInventoryItem(target);
        target.setOnBridge(-10L);
        target.setMailed(true);
        target.setLastMaintained(WurmCalendar.currentTime);
    }

    private String getFullName(Item item) {
        StringBuilder sb = new StringBuilder();

        switch (item.getRarity()) {
            case 1:
                sb.append("rare ");
                break;
            case 2:
                sb.append("supreme ");
                break;
            case 3:
                sb.append("fantastic ");
                break;
        }

        MaterialUtilities.appendNameWithMaterialSuffix(sb,
                item.getName().equals("") ? item.getTemplate().getName() : item.getName(),
                item.getMaterial());
        return sb.toString();
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId) {
            Item source;
            try {
                source = Items.getItem(action.getSubjectId());
            } catch (NoSuchItemException e) {
                performer.getCommunicator().sendNormalServerMessage("The spirits fly around in circles looking confused.");
                logger.warning("Could not get contract item (" + action.getSubjectId() + ") for some reason.  Possible explanation follows:");
                e.printStackTrace();
                return true;
            }
            if (source.getItemCount() == 0) {
                Item[] toPack = new Item[0];
                if (target.getTemplateId() == ItemList.itemPile) {
                    toPack = target.getItemsAsArray();
                } else if (target.getParentOrNull() == null) {
                    toPack = new Item[] {target};
                } else {
                    // Inventory groups by something similar to getFullName.
                    String targetName = getFullName(target);
                    Item parent = target.getParentOrNull();
                    List<Item> items = new ArrayList<>();
                    if (parent != null) {
                        for (Item item : parent.getItems()) {
                            if (getFullName(item).equals(targetName)) {
                                items.add(item);
                            }
                        }

                        toPack = items.toArray(new Item[0]);
                    }
                }
                if (toPack.length > 0) {
                    if (toPack.length > 99) {
                        performer.getCommunicator().sendNormalServerMessage("It would not be possible to unpack that many items at the destination.");
                        return true;
                    }

                    for (Item item : toPack) {
                        PackResult result = checkTake(performer, item);
                        if (!result.wasSuccessful()) {
                            result.sendToPerformer(performer);
                            return true;
                        }
                    }

                    boolean errorWhenTaking = false;

                    boolean mixedItems = false;
                    int itemCount = 0;
                    float totalQL = 0;
                    float allSameQL = toPack[0].getQualityLevel();
                    for (Item item : toPack) {
                        try {
                            actuallyTake(performer, item, source);
                        } catch (NoSuchItemException | NoSuchZoneException e) {
                            errorWhenTaking = true;
                            e.printStackTrace();
                            continue;
                        }

                        ++itemCount;
                        totalQL += item.getQualityLevel();
                        if (allSameQL != -1 && item.getQualityLevel() != allSameQL)
                            allSameQL = -1;

                        if (!mixedItems && item.getTemplateId() != toPack[0].getTemplateId())
                            mixedItems = true;
                    }

                    StringBuilder description = new StringBuilder();

                    if (mixedItems) {
                        description.append("mixed items x ").append(itemCount);
                    } else {
                        description.append(getFullName(toPack[0])).append(" (");
                        if (itemCount == 1) {
                            description.append(totalQL).append("ql)");
                        } else {
                            if (allSameQL == -1)
                                description.append("avg. ");
                            description.append(totalQL / itemCount).append("ql) x ").append(itemCount);
                        }
                    }

                    source.setName("delivery note");
                    source.setDescription(description.toString());

                    boolean isManyItems = toPack.length > 1;
                    performer.getCommunicator().sendNormalServerMessage(String.format("The spirits take the %s with a promise to return %s to the bearer of this note.",
                            (isManyItems ? "items" : "item"),
                            (isManyItems ? "them" : "it")));
                    if (errorWhenTaking)
                        // TODO - Better message?
                        performer.getCommunicator().sendNormalServerMessage("An error occurred when packing, some of the items may not have been packed.");
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
