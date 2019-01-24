package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.gotti.wurmunlimited.modsupport.actions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class DeliverAction implements ModAction, BehaviourProvider, ActionPerformer {
    private static final Logger logger = Logger.getLogger(DeliverAction.class.getName());
    private final short actionId;
    private final ActionEntry actionEntry;
    private final short dropAsPileId = (short)638;

    public DeliverAction() {
        actionId = (short)ModActions.getNextActionId();

        actionEntry = new ActionEntryBuilder(actionId, "Deliver Here", "delivering here",
                new int[] {
                      ActionTypes.ACTION_TYPE_ALWAYS_USE_ACTIVE_ITEM,
                      ActionTypes.ACTION_TYPE_QUICK
                }).range(4).build();

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
            if (subject.getTemplateId() == DeliveryContractsMod.getTemplateId() && subject.getItemCount() > 0) {
                if (target.getTemplateId() == ItemList.villageToken || (target.getTemplateId() == ItemList.waystone && target.isPlanted())) {
                    return Collections.singletonList(actionEntry);
                }
            }
        }
        return null;
    }

    @Override
    public boolean action(Action action, Creature performer, Item target, short num, float counter) {
        if (num == actionId) {
            Village village = Zones.getVillage(performer.getTilePos(), performer.isOnSurface());
            if (village != null && !village.getRoleFor(performer).mayDrop()) {
                performer.getCommunicator().sendNormalServerMessage("You would not have permission to pickup the items after delivery.");
                return false;
            }

            try {
                Item source = Items.getItem(action.getSubjectId());
                if (source.getItemCount() > 0) {

//                    if (performer.currentTile.getNumberOfItems(performer.getFloorLevel(true)) + source.getItemCount() > 99) {
//                        performer.getCommunicator().sendNormalServerMessage("The area is too littered with items already.");
//                    } else {
                    Item[] items = source.getItemsAsArray();
                    DeliveryContractsMod.addWeightToBlock(performer, Arrays.stream(items).mapToInt(Item::getWeightGrams).sum());
                    ItemBehaviour b = (ItemBehaviour)Behaviours.getInstance().getBehaviour(BehaviourList.itemBehaviour);
                    b.action(null, performer, items, dropAsPileId, 0);
//                        for (Item item : source.getItems().toArray(new Item[0])) {
//                            DeliveryContractsMod.addWeightToBlock(performer, item.getWeightGrams());
//                            // TODO - Onepertile
//                            // TODO - Fourpertile
//                            // TODO - Already dropped items not added to volatile items layer for some reason.
//
//
////                            if (MethodsItems.drop(performer, item, false).length == 0) {
////                                logger.warning("Failure");
////                            }
//                            //item.putItemInfrontof(performer);
//                        }
                        //Items.destroyItem(source.getWurmId());
                    if (source.getItemCount() == items.length) {
                        return true;
                    } else if (source.getItemCount() == 0) {
                        source.setName("delivery contract");
                        source.setDescription("");
                        performer.getCommunicator().sendNormalServerMessage("The spirits place the item" + (items.length == 1 ? "" : "s") + " in front of you.");
                    } else {
                        DeliveryContractsMod.removeWeightToBlock(performer, source.getItems().stream().mapToInt(Item::getWeightGrams).sum());
                        // TODO - "Some of the item(s)".
                        performer.getCommunicator().sendNormalServerMessage("The spirits place the item" + (source.getItemCount() == 1 ? "" : "s") + " in front of you.");
                    }

                    return true;
                }
            } catch (NoSuchItemException | NoSuchBehaviourException e) {
                performer.getCommunicator().sendNormalServerMessage("The spirits fly around in circles looking confused.");
                // TODO - Change.  re behaviour.
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
