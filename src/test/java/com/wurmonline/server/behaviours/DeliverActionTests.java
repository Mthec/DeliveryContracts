package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.VolaTile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeliverActionTests extends ActionBehaviourTest {
    private DeliverAction mod = new DeliverAction((short)1);

    @Test
    void testGetActionId() {
        short actionId = 34;
        DeliverAction action = new DeliverAction(actionId);
        assertEquals(actionId, action.getActionId());
    }

    // getBehaviourFor

    @Test
    void testGetBehaviourFor() {
        contract.setData(1);
        assertNotNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForNotSetContract() {
        contract.setData(-1);
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForWrongItemType() {
        contract.setData(1);
        contract.setTemplateId(contractTemplateId + 1);
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForDestinations() {
        contract.setData(1);
        assertNotNull(mod.getBehavioursFor(creature, contract, waystone));
        assertNotNull(mod.getBehavioursFor(creature, contract, villageToken));
        assertNull(mod.getBehavioursFor(creature, contract, new Item(ItemList.itemPile)));
    }

    // action

    @Test
    void testActionContractDestroyed() {
        contract.setData(pile.getWurmId());
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertTrue(Items.wasDestroyed(contract));
    }

    @Test
    void testActionContractNotDestroyedIfDataNotSet() {
        contract.setData(-1);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(Items.wasDestroyed(contract));
    }

    @Test
    void testActionSmallItemsAreInserted() {
        Item smallItem = new Item(ItemList.acorn);
        contract.setData(smallItem.getWurmId());
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertTrue(creature.getInventory().getItems().contains(smallItem));
        assertTrue(creature.getCommunicator().getLastMessage().contains("deliver"));
    }

    @Test
    void testActionPileOfSmallItemsAreInserted() {
        for (int i = 0; i < 10; i++) {
            pile.insertItem(new Item(ItemList.acorn));
        }
        Item[] items = pile.getItemsAsArray();
        contract.setData(pile.getWurmId());
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(creature.getInventory().getItems().contains(pile));
        assertEquals(items.length, creature.getInventory().getItemCount());
        assertTrue(creature.getCommunicator().getLastMessage().contains("deliver"));
    }

    @Test
    void testActionLargeItemsArePlacedInFrontOf() {
        contract.setData(pile.getWurmId());
        pile.setWeight(100);
        creature.setCarry(5);
        creature.currentTile = new VolaTile(0);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(creature.getInventory().getItems().contains(pile));
        assertTrue(pile.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("place"));
    }

    @Test
    void testWillNotDeliverToDifferentVillage() {
        contract.setData(pile.getWurmId());
        villageToken.setVillageTokenId(creature.getVillageId() + 1);
        mod.action(action, creature, villageToken, mod.getActionId(), 0);

        assertFalse(pile.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("will not deliver"));
    }

    @Test
    void testWillDeliverToPlayerVillage() {
        contract.setData(pile.getWurmId());
        villageToken.setVillageTokenId(creature.getVillageId());
        pile.setWeight(100);
        creature.setCarry(1);
        creature.currentTile = new VolaTile(0);
        mod.action(action, creature, villageToken, mod.getActionId(), 0);

        assertTrue(pile.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits place"));
    }

    @Test
    void testSingleDroppedItemNotDestroyedOnDelivery() {
        Item notPile = new Item(ItemList.forge);
        notPile.setWeight(100);
        creature.setCarry(10);
        contract.setData(notPile.getWurmId());
        creature.currentTile = new VolaTile(0);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertTrue(notPile.isInFrontOf(creature));
        assertFalse(Items.wasDestroyed(notPile));
    }

    @Test
    void testLargeItemsTooManyItemsInFrontOf() {
        contract.setData(pile.getWurmId());
        pile.insertItem(new Item(ItemList.acorn));
        pile.setWeight(20);
        creature.setCarry(10);
        creature.currentTile = new VolaTile(99);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(creature.getInventory().getItems().contains(pile));
        assertFalse(pile.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("littered"));
    }
}
