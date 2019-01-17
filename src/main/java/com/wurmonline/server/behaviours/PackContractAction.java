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
import mod.wurmunlimited.delivery.DeliveryContractsMod;
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
                }).range(4).build();

        ModActions.registerAction(actionEntry);
    }

    // For testing.
    PackContractAction(short actionId) {
        this.actionId = actionId;
        actionEntry = null;
    }

    private boolean canPack(Creature performer, Item item) {
        if (item.getOwnerId() != performer.getWurmId() && item.getOwnerId() != -10)
            return false;
        if (item.isInventory() || item.isBodyPart() || item.isBeingWorkedOn() || item.isCoin() || item.isMailed() ||
                    item.isLiquid() || item.isFullprice() || item.isBanked() || item.isBulkItem() ||
                    (item.isBulkContainer() && item.getBulkNums() != 0) || !item.canBeDropped(true) ||
                    item.getBridgeId() != performer.getBridgeId() || MethodsItems.checkIfStealing(item, performer, null) ||
                    Blocking.getBlockerBetween(performer, item, 4) != null)
            return false;
        Village v = Zones.getVillage(item.getTilePos(), item.isOnSurface());
        if (v != null) {
            VillageRole roles = v.getRoleFor(performer);
            if (roles != null) {
                if ((item.isPlanted() && !roles.mayPickupPlanted()) || !roles.mayPickup())
                    return false;
            }
        }
        return true;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        if (subject != null && target != null) {
            if (subject.getTemplateId() == DeliveryContractsMod.getTemplateId() && subject.getItemCount() == 0 && canPack(performer, target)) {
                return Collections.singletonList(actionEntry);
            }
        }
        return null;
    }

    private TakeResultEnum checkTake(Creature performer, Item target) {
        if (target.isBusy()) {
            TakeResultEnum.TARGET_IN_USE.setIndexText(performer.getWurmId(), target.getName());
            return TakeResultEnum.TARGET_IN_USE;
        } else {
            long ownId = target.getOwnerId();
            if (ownId != -10L) {
                return TakeResultEnum.TARGET_HAS_NO_OWNER;
            }

            if (ownId == performer.getWurmId()) {
                return TakeResultEnum.PERFORMER_IS_OWNER;
            }
            if (target.isCoin()) {
                // TODO - Replace with no coins allowed.
                return TakeResultEnum.INVENTORY_FULL;
            }

            if (target.mailed) {
                return TakeResultEnum.TARGET_IS_UNREACHABLE;
            }

            if (target.isLiquid()) {
                return TakeResultEnum.TARGET_IS_LIQUID;
            }

            if ((target.isBulkContainer() || target.isTent()) && !target.isEmpty(true)) {
                return TakeResultEnum.TARGET_FILLED_BULK_CONTAINER;
            }

            if (target.isBulkItem()) {
                return TakeResultEnum.TARGET_BULK_ITEM;
            }

            if (target.isTent()) {
                Vehicle vehicle = Vehicles.getVehicle(target);
                if (vehicle != null && vehicle.getDraggers() != null && vehicle.getDraggers().size() > 0) {
                    return TakeResultEnum.HITCHED;
                }
            }

            try {
                BlockingResult result = Blocking.getBlockerBetween(performer, target, 4);
                if (result != null) {
                    TakeResultEnum.TARGET_BLOCKED.setIndexText(performer.getWurmId(), target.getName(), result.getFirstBlocker().getName());
                    return TakeResultEnum.TARGET_BLOCKED;
                }

                if (!target.isNoTake()) {
                    boolean sameVehicle = false;
                    Item top = target.getTopParentOrNull();
                    if (top != null && top.isVehicle() && top.getWurmId() == performer.getVehicle()) {
                        sameVehicle = true;
                    }

                    if (!sameVehicle && !performer.isWithinDistanceTo(target.getPosX(), target.getPosY(), target.getPosZ(), 5.0F)) {
                        TakeResultEnum.TOO_FAR_AWAY.setIndexText(performer.getWurmId(), target.getName());
                        return TakeResultEnum.TOO_FAR_AWAY;
                    }

                    Zone tzone = Zones.getZone((int)target.getPosX() >> 2, (int)target.getPosY() >> 2, target.isOnSurface());
                    VolaTile tile = tzone.getTileOrNull((int)target.getPosX() >> 2, (int)target.getPosY() >> 2);
                    if (tile != null) {
                        Structure struct = tile.getStructure();
                        VolaTile tile2 = performer.getCurrentTile();
                        if (tile2 != null) {
                            if (tile.getStructure() != struct && (struct == null || !struct.isTypeBridge())) {
                                performer.getCommunicator().sendNormalServerMessage("You can't reach the " + target.getName() + " through the wall.");
                                return TakeResultEnum.TARGET_BLOCKED;
                            }
                        } else if (struct != null && !struct.isTypeBridge()) {
                            performer.getCommunicator().sendNormalServerMessage("You can't reach the " + target.getName() + " through the wall.");
                            return TakeResultEnum.TARGET_BLOCKED;
                        }
                    }

                    long toppar = target.getTopParent();
                    if (!MethodsItems.isLootableBy(performer, target)) {
                        return TakeResultEnum.MAY_NOT_LOOT_THAT_ITEM;
                    }

                    boolean mayUseVehicle = true;

                    Item stealing;
                    try {
                        stealing = Items.getItem(toppar);
                        if (stealing.isDraggable()) {
                            mayUseVehicle = MethodsItems.mayUseInventoryOfVehicle(performer, stealing);
                        }

                        if (!mayUseVehicle && target.lastOwner != performer.getWurmId() && (stealing.isVehicle() && stealing.getLockId() != -10L || Items.isItemDragged(stealing)) && performer.getDraggedItem() != stealing) {
                            TakeResultEnum.VEHICLE_IS_WATCHED.setIndexText(performer.getWurmId(), stealing.getName());
                            return TakeResultEnum.VEHICLE_IS_WATCHED;
                        }
                    } catch (NoSuchItemException ignored) {}

                    if (MethodsItems.checkIfStealing(target, performer, null)) {
                        TakeResultEnum.NEEDS_TO_STEAL.setIndexText(performer.getWurmId(), target.getName());
                        return TakeResultEnum.NEEDS_TO_STEAL;
                    }

                    return TakeResultEnum.SUCCESS;
                }
            } catch (NoSuchZoneException var22) {
                logger.log(Level.WARNING, var22.getMessage(), var22);
            }
        }
        return TakeResultEnum.UNKNOWN_FAILURE;
    }
    private void actuallyTake(Creature performer, Item target, Item contract) throws Exception {
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

        if (target.isPlanted() && (target.isSign() || target.isStreetLamp() || target.isFlag() || target.isBulkContainer() || target.getTemplateId() == 742)) {
            target.setIsPlanted(false);
            if (target.isAbility()) {
                target.hatching = false;
                target.setRarity((byte)0);
            }
        }

        if (target == performer.getDraggedItem()) {
            performer.setDraggedItem(null);
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
        target.setOnBridge(-10L);
        target.setLastMaintained(WurmCalendar.currentTime);
    }

    // TODO - Warning message when one item but multiple selected?  Only when item is in inventory?
    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId && canPack(performer, target)) {
            try {
                if (target.getTemplateId() == ItemList.inventory) {
                    throw new NoSuchItemException("Attempted to pack an inventory.");
                }
                Item source = Items.getItem(action.getSubjectId());
                if (source.getItemCount() == 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(target.getName()).append(" (");

                    Item[] toPack = new Item[0];
                    if (target.getTemplateId() == ItemList.itemPile) {
                        toPack = target.getItemsAsArray();
                    } else {
                        Item parent = target.getParentOrNull();
                        List<Item> items = null;
                        if (parent != null) {
                            for (Item item : parent.getItemsAsArray()) {
                                if (item.getTemplateId() == target.getTemplateId()) {
                                    if (items == null && item == target) {
                                        items = new ArrayList<>();
                                    } else {
                                        break;
                                    }

                                    items.add(item);
                                }
                            }

                            if (items != null) {
                                toPack = items.toArray(new Item[0]);
                            }
                        }
                    }
                    if (toPack.length > 0) {
                        if (toPack.length > 99) {
                            performer.getCommunicator().sendNormalServerMessage("It would not be possible to unpack that many items at the destination.");
                            return true;
                        }

                        for (Item item : toPack) {
                            TakeResultEnum result = checkTake(performer, item);
                            if (result != TakeResultEnum.SUCCESS) {
                                result.sendToPerformer(performer);
                                return true;
                            }
                        }

                        int itemCount = 0;
                        float totalQL = 0;
                        float allSameQL = toPack[0].getQualityLevel();
                        for (Item item : toPack) {
                            ++itemCount;
                            totalQL += item.getQualityLevel();
                            if (allSameQL != -1 && item.getQualityLevel() != allSameQL)
                                allSameQL = -1;

                            try {
                                actuallyTake(performer, item, source);
                            } catch (Exception e) {
                                // TODO - Set error boolean?
                                e.printStackTrace();
                            }
                        }

                        if (allSameQL == -1)
                            sb.append("avg. ");
                        sb.append(totalQL / itemCount).append("ql) x ").append(itemCount);
                    } else {
                        sb.append(target.getQualityLevel()).append("ql)");
                    }

                    // TODO - Any better value?
                    source.setData(1);

                    source.setName("delivery note");
                    source.setDescription(sb.toString());

                    boolean isManyItems = toPack.length > 0;
                    performer.getCommunicator().sendNormalServerMessage(String.format("The spirits take the %s with a promise to return %s to the bearer of this note.",
                            (isManyItems ? "items" : "item"),
                            (isManyItems ? "them" : "it")));
                    return true;
                }
            } catch (NoSuchItemException e) {
                performer.getCommunicator().sendNormalServerMessage("The spirits fly around in circles looking confused.");
                logger.warning("An error occurred when trying to pack a delivery contract.  Possible explanation follows:");
                e.printStackTrace();
                return true;
            }
        }
        return false;
    }

    @Override
    public short getActionId() {
        return actionId;
    }
}
