package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.NoSuchZoneException;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class DeliverAction implements ModAction, BehaviourProvider, ActionPerformer {
    private static final Logger logger = Logger.getLogger(DeliverAction.class.getName());
    private final short actionId;
    private final ActionEntry actionEntry;

    public DeliverAction() {
        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "Deliver Here", "delivering here",
                new int[] {
                      ActionTypes.ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM,
                      ActionTypes.ACTION_TYPE_QUICK
                }).build();

        ModActions.registerAction(actionEntry);
    }

    // For testing.
    DeliverAction(short id) {
        actionId = id;
        actionEntry = null;
    }

    @Override
    public List<ActionEntry> getBehavioursFor(Creature performer, Item subject, Item target) {
        if (subject != null && target != null) {
            if (subject.getTemplateId() == DeliveryContractsMod.getTemplateId() && subject.getData() != -1L) {
                if (target.getTemplateId() == ItemList.villageToken || target.getTemplateId() == ItemList.waystone) {
                    return Collections.singletonList(actionEntry);
                }
            }
        }
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId) {
            if (target.getTemplateId() == ItemList.villageToken && target.getData2() != performer.getVillageId()) {
                performer.getCommunicator().sendNormalServerMessage("The spirits will not deliver to a village you aren't a citizen of.");
                return false;
            }
            try {
                Item source = Items.getItem(action.getSubjectId());
                long itemId = source.getData();
                if (itemId != -1L){
                    Item toDeliver = Items.getItem(itemId);
                    int itemTemplateId = toDeliver.getTemplateId();
                    if (itemTemplateId == ItemList.inventory && !toDeliver.getName().equals(PackContractAction.fakeInventoryName)) {
                        throw new NoSuchItemException("A real inventory was packed into delivery contract some how.");
                    }

                    if (performer.canCarry(toDeliver.getFullWeight())) {
                        if (itemTemplateId == ItemList.itemPile || itemTemplateId == ItemList.inventory) {
                            for (Item item : toDeliver.getItems()) {
                                performer.getInventory().insertItem(item);
                            }
                            Items.destroyItem(toDeliver.getWurmId());
                        } else {
                            performer.getInventory().insertItem(toDeliver);
                        }
                        Items.destroyItem(source.getWurmId());
                        performer.getCommunicator().sendNormalServerMessage("The spirits deliver the item" + (toDeliver.getItemCount() <= 1 ? "" : "s") + " to you.");
                        return true;

                    } else if (performer.currentTile.getNumberOfItems(performer.currentTile.getDropFloorLevel(performer.getFloorLevel(true))) + (target.getItemCount() == 0 ? 1 : target.getItemCount()) > 99) {
                        performer.getCommunicator().sendNormalServerMessage("The area is too littered with items already.");
                        return true;

                    } else {
                        if (itemTemplateId == ItemList.inventory) {
                            for (Item item : toDeliver.getItems()) {
                                item.putItemInfrontof(performer);
                            }
                            Items.destroyItem(toDeliver.getWurmId());
                        } else {
                            toDeliver.putItemInfrontof(performer);
                        }
                        Items.destroyItem(source.getWurmId());
                        performer.getCommunicator().sendNormalServerMessage("The spirits place the item" + (toDeliver.getItemCount() <= 1 ? "" : "s") + " in front of you.");
                        return true;
                    }
                }
            } catch (NoSuchItemException | NoSuchCreatureException | NoSuchPlayerException | NoSuchZoneException e) {
                performer.getCommunicator().sendNormalServerMessage("The spirits fly around in circles looking confused.");
                logger.warning("An error occurred when unpacking " + action.getSubjectId() + ".  Some items may not have been placed.");
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
