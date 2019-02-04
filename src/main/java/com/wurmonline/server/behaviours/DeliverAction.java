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

    private void unMarkItemAndSubItems(Item item) {
        item.setMailed(false);
        if (DeliveryContractsMod.setNoDecay) {
            String description = item.getDescription();
            if (description.endsWith("*"))
                item.setDescription(description.substring(0, description.length() - 1));
            else
                item.setHasNoDecay(false);
        } else if (DeliveryContractsMod.setNoDecayFood && item.isFood())
            item.setHasNoDecay(false);

        for (Item subItem : item.getItems()) {
            unMarkItemAndSubItems(subItem);
        }
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
                    Item[] items = source.getItemsAsArray();
                    DeliveryContractsMod.addWeightToBlock(performer, Arrays.stream(items).mapToInt(Item::getWeightGrams).sum());
                    ItemBehaviour b = (ItemBehaviour)Behaviours.getInstance().getBehaviour(BehaviourList.itemBehaviour);
                    b.action(null, performer, items, Actions.DROP_AS_PILE, 0);
                    Arrays.stream(items).filter(item -> !source.getItems().contains(item))
                            .forEach(this::unMarkItemAndSubItems);

                    if (source.getItemCount() == items.length) {
                        // Message sent via ItemBehaviour.
                        return true;
                    } else if (source.getItemCount() == 0) {
                        Items.destroyItem(source.getWurmId());
                        // For testing.
//                        source.setName("delivery contract");
//                        source.setDescription("");
                        performer.getCommunicator().sendNormalServerMessage("The spirits place the item" + (items.length == 1 ? "" : "s") + " in front of you.");
                    } else {
                        source.setDescription("remaining items x " + source.getItemCount());
                        DeliveryContractsMod.removeWeightToBlock(performer, source.getItems().stream().mapToInt(Item::getWeightGrams).sum());
                        performer.getCommunicator().sendNormalServerMessage("The spirits place some of the items in front of you.");
                    }

                    return true;
                }
            } catch (NoSuchItemException | NoSuchBehaviourException e) {
                performer.getCommunicator().sendNormalServerMessage("The spirits fly around in circles looking confused.");
                logger.warning("An error occurred when unpacking " + action.getSubjectId() + ":");
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
