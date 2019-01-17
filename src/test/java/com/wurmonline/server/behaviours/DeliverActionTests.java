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
        contract.insertItem(itemToPack);
        assertNotNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForNotSetContract() {
        assert contract.getItemCount() == 0;
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForWrongItemType() {
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
        villageToken.setVillageTokenId(creature.getVillageId() + 1);
        mod.action(action, creature, villageToken, mod.getActionId(), 0);

        assertFalse(itemToPack.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("will not deliver"));
    }

    @Test
    void testWillDeliverToPlayerVillage() {
        contract.insertItem(itemToPack, true);
        villageToken.setVillageTokenId(creature.getVillageId());
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
        creature.currentTile = new VolaTile(99);
        mod.action(action, creature, waystone, mod.getActionId(), 0);

        assertFalse(creature.getInventory().getItems().contains(itemToPack));
        assertFalse(itemToPack.isInFrontOf(creature));
        assertTrue(creature.getCommunicator().getLastMessage().contains("littered"));
    }
}
