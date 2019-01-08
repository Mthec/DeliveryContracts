package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.Blocking;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.VillageRole;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;
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
                }).build();

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
            if (subject.getTemplateId() == DeliveryContractsMod.getTemplateId() && subject.getData() == -1L && canPack(performer, target)) {
                return Collections.singletonList(actionEntry);
            }
        }
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId && canPack(performer, target)) {
            try {
                if (target.getTemplateId() == ItemList.inventory) {
                    throw new NoSuchItemException("Attempted to pack an inventory.");
                }
                Item source = Items.getItem(action.getSubjectId());
                if (source.getData() == -1L) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(target.getName());
                    sb.append(" (");

                    if (target.getTemplateId() == ItemList.itemPile) {
                        int itemCount = 0;
                        float totalQL = 0;
                        Item[] items = target.getItems().toArray(new Item[0]);
                        if (items.length > 99) {
                            performer.getCommunicator().sendNormalServerMessage("It would not be possible to unpack that many items at the destination.");
                            return true;
                        }

                        float allSameQL = items[0].getQualityLevel();
                        for (Item item : items) {
                            ++itemCount;
                            totalQL += item.getQualityLevel();
                            if (allSameQL != -1 && item.getQualityLevel() != allSameQL)
                                allSameQL = -1;
                        }

                        if (allSameQL == -1)
                            sb.append("avg. ");
                        sb.append(totalQL / itemCount);
                        sb.append("ql) x ");
                        sb.append(itemCount);
                    } else {
                        sb.append(target.getQualityLevel());
                        sb.append("ql)");
                    }
                    source.setData(target.getWurmId());
                    target.putInVoid();

                    source.setName("delivery note");
                    source.setDescription(sb.toString());
                    performer.getCommunicator().sendNormalServerMessage("The spirits take the items with a promise to return them.");
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
