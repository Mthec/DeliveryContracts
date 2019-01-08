package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.Blocking;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackContractActionTests extends ActionBehaviourTest {
    private PackContractAction mod = new PackContractAction((short)1);

    @Test
    void testGetActionId() {
        short actionId = 34;
        PackContractAction action = new PackContractAction(actionId);
        assertEquals(actionId, action.getActionId());
    }

    // getBehavioursFor

    @Test
    void testGetBehaviourFor() {
        contract.setData(1);
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForNotSetContract() {
        contract.setData(-1);
        assertNotNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForWrongItemType() {
        contract.setData(1);
        contract.setTemplateId(contractTemplateId + 1);
        assertNull(mod.getBehavioursFor(creature, contract, waystone));
    }

    @Test
    void testGetBehaviourForItem() {
        assertNotNull(mod.getBehavioursFor(creature, contract, pile));
    }

    // action

    @Test
    void testContractNotFound() {
        // TODO - Testing logging?
    }

    @Test
    void testContractDataSetProperly() {
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertEquals(pile.getWurmId(), contract.getData());
    }

    @Test
    void testNameChangeWhenAssigned() {
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertEquals("delivery note", contract.getName());
    }

    @Test
    void testPlayerMessageOnSuccess() {
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits take"));
    }

    @Test
    void testItemSentToVoid() {
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertTrue(pile.inTheVoid());
    }

    @Test
    void testDescriptionSetForSingleItem() {
        Item toDeliver = new Item(ItemList.itemPile + 1);
        float ql = 25;
        toDeliver.setName("Test");
        toDeliver.setQualityLevel(25);
        mod.action(action, creature, toDeliver, mod.getActionId(), 0);

        assertEquals("Test (" + ql + "ql)", contract.getDescription());
    }

    @Test
    void testDescriptionSetForPileItemSameQL() {
        float ql = 25;
        int num = 10;
        pile.getItems().clear();
        for (int i = 0; i < num; i++) {
            Item newItem = new Item(ItemList.ironBar);
            newItem.setQualityLevel(ql);
            pile.insertItem(newItem);
        }
        pile.setName("Test");
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertEquals("Test (" + ql + "ql) x " + num, contract.getDescription());
    }

    @Test
    void testDescriptionSetForPileItemDifferentQL() {
        float ql = 25;
        int num = 10;
        pile.getItems().clear();
        for (int i = -num; i < num; i++) {
            Item newItem = new Item(ItemList.ironBar);
            newItem.setQualityLevel(ql + i);
            pile.insertItem(newItem);
        }
        pile.setName("Test");
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertEquals("Test (avg. " + pile.getItems().stream().mapToDouble(Item::getQualityLevel).average().orElseThrow(RuntimeException::new) + "ql) x " + num * 2, contract.getDescription());
    }

    @Test
    void testAttemptingToPackPlayerInventory() {
        Item inventory = new Item(ItemList.inventory);
        mod.action(action, creature, inventory, mod.getActionId(), 0);

        assertTrue(creature.getCommunicator().getLastMessage().contains("in circles"));
    }

    // action Item[]

    @Test
    void testPackingTooManyItems() {
        for (int i = 0; i < 100; i++) {
            pile.insertItem(new Item(ItemList.acorn));
        }

        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
        assertTrue(creature.getCommunicator().getLastMessage().contains("that many items"));
    }

    // canPack

    @Test
    void testBlockItemOwnedByOther() {
        pile.setOwnerId(creature.getWurmId() + 1);
        assertFalse(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    @Test
    void testNotBlockItemOwnedByContractUserOrNobody() {
        pile.setOwnerId(creature.getWurmId());
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
        contract.setData(-1);
        pile.setOwnerId(-10);
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    @Test
    void testBlockItemIfCannotReach() {
        Blocking.blocked = true;
        assertFalse(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    @Test
    void testBlockIfDoesNotHaveVillagePermissionToPickup() {
        village.getRoleFor(creature).setMayPickup(false);
        assertFalse(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    @Test
    void testNotBlockIfDoesHaveVillagePermissionToPickup() {
        village.getRoleFor(creature).setMayPickup(true);
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    @Test
    void testBlockIfDoesNotHaveVillagePermissionToPickupPlanted() {
        village.getRoleFor(creature).setMayPickup(false);
        village.getRoleFor(creature).setMayPickupPlanted(false);
        pile.setPlanted(true);
        assertFalse(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    @Test
    void testNotBlockIfDoesHaveVillagePermissionToPickupPlanted() {
        village.getRoleFor(creature).setMayPickupPlanted(true);
        pile.setPlanted(true);
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    private void trueCheck() {
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    private void falseCheck() {
        assertFalse(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    private void falseTrueCheck() {
        falseCheck();
        trueCheck();
    }

    @Test
    void testManyBlockOptions() {
        contract = spy(contract);
        when(contract.getData()).thenReturn(-1L);
        pile = spy(pile);
        when(pile.getBridgeId()).thenReturn(creature.getBridgeId());
        village.getRoleFor(creature).setMayPickup(true);

        Items.reset();
        Items.addItems(contract, pile);

        when(pile.isInventory()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isBodyPart()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isBeingWorkedOn()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isCoin()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isMailed()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isLiquid()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isFullprice()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isBanked()).thenReturn(true, false);
        falseTrueCheck();
        when(pile.isBulkItem()).thenReturn(true, false);
        falseTrueCheck();

        when(pile.isBulkContainer()).thenReturn(true, true, false);
        when(pile.getBulkNums()).thenReturn(5, 0);
        falseTrueCheck();

        when(pile.canBeDropped(anyBoolean())).thenReturn(false, true);
        falseTrueCheck();

        when(pile.getBridgeId()).thenReturn(creature.getBridgeId() + 1, creature.getBridgeId());
        falseTrueCheck();

        MethodsItems.isStealing = true;
        falseCheck();

        MethodsItems.isStealing = false;
        trueCheck();
    }
}
