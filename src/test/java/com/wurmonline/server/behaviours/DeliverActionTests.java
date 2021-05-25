package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.VolaTile;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliverActionTests extends ActionBehaviourTest {
    private final DeliverAction mod = new DeliverAction((short)1);

    @Test
    void testGetActionId() {
        short actionId = 34;
        DeliverAction action = new DeliverAction(actionId);
        assertEquals(actionId, action.getActionId());
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourFor() {
        contract.insertItem(itemToPack);
        assertNotNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForNotSetContract() {
        assert contract.getItemCount() == 0;
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForWrongContractItemType() {
        contract.insertItem(itemToPack);
        contract.setTemplateId(contractTemplateId + 1);
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForDestinations() {
        contract.insertItem(itemToPack);
        assertNotNull(mod.getBehavioursFor(creature, contract, waystone));
        assertNotNull(mod.getBehavioursFor(creature, contract, villageToken));
        assertNull(mod.getBehavioursFor(creature, contract, new Item(ItemList.itemPile)));
    }

    @Test
    void testGetBehaviourForNotPlantedWaystone() {
        contract.insertItem(itemToPack);
        Item notPlantedWaystone = new Item(ItemList.waystone);
        assert !notPlantedWaystone.isPlanted();
        assertNull(mod.getBehavioursFor(creature, contract, notPlantedWaystone));
    }

    @Test
    void testGetBehavioursForBuoy() {
        contract.insertItem(itemToPack);
        itemToPack.setTemplateId(ItemList.caravel);
        Item buoy = new Item(ItemList.buoy);
        buoy.setIsPlanted(true);
        assertNotNull(mod.getBehavioursFor(creature, contract, buoy));
    }

    @Test
    void testGetBehavioursForNotPlantedBuoy() {
        contract.insertItem(itemToPack);
        itemToPack.setTemplateId(ItemList.caravel);
        Item notPlantedBuoy = new Item(ItemList.buoy);
        assert !notPlantedBuoy.isPlanted();
        assertNull(mod.getBehavioursFor(creature, contract, notPlantedBuoy));
    }

    @Test
    void testGetBehavioursForBuoyNotBoat() {
        contract.insertItem(itemToPack);
        itemToPack.setTemplateId(ItemList.cartLarge);
        Item buoy = new Item(ItemList.buoy);
        buoy.setIsPlanted(true);
        assertNull(mod.getBehavioursFor(creature, contract, buoy));
    }

    // action

    @Test
    void testActionContractDestroyed() {
        contract.insertItem(itemToPack);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertTrue(Items.wasDestroyed(contract));
    }

    @Test
    void testActionContractNotDestroyedIfEmpty() {
        assert contract.getItemCount() == 0;
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(Items.wasDestroyed(contract));
    }

    @Test
    void testMissingContract() {
        action.subjectId = -10;
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertTrue(creature.getCommunicator().getLastMessage().contains("in circles"));
    }

    @Test
    void testActionItemsArePlacedInFrontOf() {
        contract.insertItem(itemToPack);
        itemToPack.setWeight(100);
        creature.setCarry(5);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(creature.getInventory().getItems().contains(itemToPack));
        assertTrue(itemToPack.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("place"));
    }

    @Test
    void testWillNotDeliverToDifferentVillage() {
        contract.insertItem(itemToPack);
        creature.setVillageId(new Village().getId());
        new Village();
        mod.action(action, creature, villageToken, mod.getActionId(), 0);

        assertFalse(itemToPack.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("have permission"));
    }

    @Test
    void testWillDeliverToPlayerVillage() {
        contract.insertItem(itemToPack, true);
        Village village = new Village();
        creature.setVillageId(village.getId());
        itemToPack.setWeight(100);
        creature.setCarry(1);
        mod.action(action, creature, villageToken, mod.getActionId(), 0);

        assertTrue(itemToPack.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits place"));
    }

    @Test
    void testTooManyItemsInFrontOf() {
        contract.insertItem(itemToPack);
        itemToPack.setWeight(20);
        creature.setCarry(10);
        creature.currentTile = new VolaTile(100);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(creature.getInventory().getItems().contains(itemToPack));
        assertFalse(itemToPack.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("littered"));
    }

    @Test
    void testSingleItemPlacedMessage() {
        contract.insertItem(itemToPack);

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertTrue(creature.getCommunicator().getLastMessage().contains("item"));
        assertFalse(creature.getCommunicator().getLastMessage().contains("items"));
    }

    @Test
    void testMultipleItemsPlacedMessage() {
        contract.insertItem(itemToPack);
        contract.insertItem(new Item(ItemList.acorn));

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertTrue(creature.getCommunicator().getLastMessage().contains("items"));
    }

    @Test
    void testDescriptionUpdatedIfNotAllItemsDelivered() {
        for (int i = 0; i < 20; i++) {
            contract.insertItem(new Item(ItemList.dirtPile));
        }
        creature.currentTile = new VolaTile(90);
        assert creature.currentTile.getNumberOfItems(0) + contract.getItemCount() > 100;
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertEquals(10, contract.getItemCount());
        assertTrue(creature.getCommunicator().getLastMessage().contains("some of the items"));
        assertEquals("remaining items x 10", contract.getDescription());
    }

    @Test
    void testHasNoDecayRemovedOnDelivery() {
        DeliveryContractsMod.setNoDecay = true;
        Item target = new Item(ItemList.dirtPile);
        target.setHasNoDecay(true);
        contract.insertItem(target);

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertFalse(target.hasNoDecay());
    }

    @Test
    void testHasNoDecayRemovedFromSubItemsOnDelivery() {
        DeliveryContractsMod.setNoDecay = true;
        Item backpack = new Item(ItemList.backPack);
        Item target = new Item(ItemList.acorn);
        backpack.insertItem(target);
        backpack.setHasNoDecay(true);
        target.setHasNoDecay(true);
        contract.insertItem(backpack);

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertFalse(backpack.hasNoDecay());
        assertFalse(target.hasNoDecay());
    }

    @Test
    void testHasNoDecayNotRemovedOnDeliveryIfAlreadyHadNoDecay() {
        DeliveryContractsMod.setNoDecay = true;
        Item target = new Item(ItemList.dirtPile);
        target.setHasNoDecay(true);

        PackContractAction packContractAction = new PackContractAction();
        packContractAction.action(action, creature, target, packContractAction.getActionId(), 0);

        assert contract.contains(target);
        assert target.hasNoDecay();

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertFalse(contract.contains(target));
        assertTrue(target.hasNoDecay());
    }

    @Test
    void testHasNoDecayRemovedFromFoodOnDeliveryIfOnlyFoodOptionSet() {
        DeliveryContractsMod.setNoDecay = false;
        DeliveryContractsMod.setNoDecayFood = true;
        Item target = new Item(ItemList.casserole);
        target.food = true;
        assert !target.hasNoDecay();

        PackContractAction packContractAction = new PackContractAction();
        packContractAction.action(action, creature, target, packContractAction.getActionId(), 0);

        assert contract.contains(target);
        assert target.hasNoDecay();

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertFalse(contract.contains(target));
        assertFalse(target.hasNoDecay());
    }

    @Test
    void testHasNoDecayRemovedFromFoodInContainerOnDeliveryIfOnlyFoodOptionSet() {
        DeliveryContractsMod.setNoDecay = false;
        DeliveryContractsMod.setNoDecayFood = true;
        Item target = new Item(ItemList.casserole);
        target.food = true;
        assert !target.hasNoDecay();
        Item bowl = new Item(ItemList.bowlPottery);
        bowl.insertItem(target);
        assert !bowl.hasNoDecay();

        PackContractAction packContractAction = new PackContractAction();
        packContractAction.action(action, creature, bowl, packContractAction.getActionId(), 0);

        assert contract.contains(bowl);
        assert target.hasNoDecay();
        assert !bowl.hasNoDecay();

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertFalse(contract.contains(bowl));
        assertFalse(target.hasNoDecay());
        assertFalse(bowl.hasNoDecay());
    }

    @Test
    void testHasNoDecayNotRemovedFromItemOnDeliveryIfOnlyFoodOptionSet() {
        DeliveryContractsMod.setNoDecay = false;
        DeliveryContractsMod.setNoDecayFood = true;
        Item target = new Item(ItemList.dirtPile);
        target.setHasNoDecay(true);

        PackContractAction packContractAction = new PackContractAction();
        packContractAction.action(action, creature, target, packContractAction.getActionId(), 0);

        assert contract.contains(target);
        assert target.hasNoDecay();

        mod.action(action, creature, waystone, mod.getActionId(), 0);
        assertFalse(contract.contains(target));
        assertTrue(target.hasNoDecay());
    }
}
