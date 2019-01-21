package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.NoSuchItemException;
import com.wurmonline.server.NoSuchPlayerException;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zones;
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
                if (source.getItemCount() > 0){

                    if (performer.currentTile.getNumberOfItems(performer.currentTile.getDropFloorLevel(performer.getFloorLevel(true))) + source.getItemCount() > 99) {
                        performer.getCommunicator().sendNormalServerMessage("The area is too littered with items already.");
                    } else {
                        for (Item item : source.getItems().toArray(new Item[0])) {
                            DeliveryContractsMod.addWeightToBlock(performer, item.getWeightGrams());
                            item.putItemInfrontof(performer);
                        }
                        //Items.destroyItem(source.getWurmId());
                        source.setName("delivery contract");
                        source.setDescription("");
                        performer.getCommunicator().sendNormalServerMessage("The spirits place the item" + (source.getItemCount() == 1 ? "" : "s") + " in front of you.");
                    }

                    return true;
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
