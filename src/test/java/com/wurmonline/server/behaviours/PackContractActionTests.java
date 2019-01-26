package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.NoSuchCreatureException;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.Blocking;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zone;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.ItemMaterials;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PackContractActionTests extends ActionBehaviourTest {
    private PackContractAction mod = new PackContractAction((short)1252);

    @Test
    void testGetActionId() {
        short actionId = 34;
        PackContractAction action = new PackContractAction(actionId);
        assertEquals(actionId, action.getActionId());
    }

    // getBehavioursFor

    @Test
    void testGetBehaviourForAlreadySetContract() {
        contract.insertItem(new Item(ItemList.acorn));
        assert contract.getItemCount() > 0;
        assertNull(mod.getBehavioursFor(creature, contract, pile));
    }

    @Test
    void testGetBehaviourForNotSetContract() {
        assert contract.getItemCount() == 0;
        assertNotNull(mod.getBehavioursFor(creature, contract, pile));
    }

    @Test
    void testGetBehaviourForWrongContractItemType() {
        assert contract.getItemCount() == 0;
        contract.setTemplateId(contractTemplateId + 1);
        assertNull(mod.getBehavioursFor(creature, contract, pile));
    }

    // action

    @Test
    void testContractNotFound() {
        // TODO - Testing logging?
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
    void testPlayerMessageForSingleItem() {
        pile.dropItem(itemToPack.getWurmId(), true);
        mod.action(action, creature, itemToPack, mod.getActionId(), 0);

        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits take"));
        assertFalse(creature.getCommunicator().getLastMessage().contains("items"));
        assertTrue(creature.getCommunicator().getLastMessage().contains(" it "));
    }

    @Test
    void testPlayerMessageForMultiple() {
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits take"));
        assertTrue(creature.getCommunicator().getLastMessage().contains("items"));
        assertTrue(creature.getCommunicator().getLastMessage().contains("them"));
    }

    @Test
    void testSuccessOnItemPile() {
        int count = pile.getItemCount();
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits take"));
        assertEquals(count, contract.getItemCount());
    }

    @Test
    void testSuccessOnInventoryGrouping() {
        List<Item> items = Arrays.asList(
                new Item(ItemList.acorn),
                new Item(ItemList.acorn),
                new Item(ItemList.acorn)
        );
        items.forEach(creature.getInventory()::insertItem);

        assertTrue(mod.action(action, creature, creature.getInventory().getItemsAsArray()[0], mod.getActionId(), 0));
        assertTrue(creature.getCommunicator().getLastMessage().contains("spirits take"));
        assertEquals(3, contract.getItemCount());
        assertTrue(contract.getItems().containsAll(items));
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
    void testDescriptionSetForMultipleItemsSameQL() {
        float ql = 25;
        int num = 10;
        pile.clear();
        for (int i = 0; i < num; i++) {
            Item newItem = new Item(ItemList.ironBar);
            newItem.setQualityLevel(ql);
            newItem.setName("Test");
            pile.insertItem(newItem);
        }
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertEquals("Test (" + ql + "ql) x " + num, contract.getDescription());
    }

    @Test
    void testDescriptionSetForMultipleItemsDifferentQL() {
        float ql = 25;
        int num = 10;
        pile.clear();
        for (int i = -num; i < num; i++) {
            Item newItem = new Item(ItemList.ironBar);
            newItem.setQualityLevel(ql + i);
            newItem.setName("Test");
            pile.insertItem(newItem);
        }
        Item[] items =  pile.getItemsAsArray();
        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertEquals("Test (avg. " + (float)Arrays.stream(items).mapToDouble(Item::getQualityLevel).average().orElseThrow(RuntimeException::new) + "ql) x " + num * 2, contract.getDescription());
    }

    @Test
    void testDescriptionNotNamedAfterPile() {
        pile.setName("Pile of items");

        mod.action(action, creature, pile, mod.getActionId(), 0);
        assertNotEquals("", contract.getDescription());
        assertFalse(contract.getDescription().startsWith("Pile"));
    }

    @Test
    void testDescriptionOfDifferentItems() {
        pile.setName("Pile of items");
        Item[] items = pile.getItemsAsArray();
        items[0].setName("salt");
        items[1].setName("sugar");

        mod.action(action, creature, pile, mod.getActionId(), 0);
        assertEquals("mixed items x " + items.length, contract.getDescription());
    }

    @Test
    void testDescriptionItemCountDoesNotIncludeErrorItems() {
        Item[] items = pile.getItemsAsArray();
        Items.fakeError(items[0]);

        mod.action(action, creature, pile, mod.getActionId(), 0);
        assertEquals("mixed items x " + items.length, contract.getDescription());
    }

    @Test
    void testPackingTooManyItems() {
        for (int i = 0; i < 100; i++) {
            pile.insertItem(new Item(ItemList.acorn));
        }

        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
        assertTrue(creature.getCommunicator().getLastMessage().contains("that many items"));
    }

    @Test
    void testAlreadyAssignedContractNotChanged() {
        Item acorn = new Item(ItemList.acorn);
        contract.insertItem(acorn);

        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
        assertTrue(contract.getItems().contains(acorn));
        assertFalse(contract.getItems().contains(pile.getItems().iterator().next()));
    }

    @Test
    void testContainerItemCanBePacked() {
        Item backpack = new Item(ItemList.backPack);
        backpack.hollow = true;
        backpack.insertItem(new Item(ItemList.acorn));

        assertTrue(mod.action(action, creature, backpack, mod.getActionId(), 0));
        assertTrue(contract.getItems().contains(backpack));
        assertEquals(1, backpack.getItemCount());
    }

    @Test
    void testItemsPackedBasedOnFullName() {
        // i.e. lumps not "lumped" together.
        int number = 20;
        Item inventory = creature.getInventory();
        for (int i = 0; i < number; i++) {
            Item lump = new Item(ItemList.goldBar);
            lump.setName("lump");
            lump.material = ItemMaterials.MATERIAL_GOLD;
            inventory.insertItem(lump);

            lump = new Item(ItemList.ironBar);
            lump.setName("lump");
            lump.material = ItemMaterials.MATERIAL_IRON;
            inventory.insertItem(lump);
        }

        Item goldLump = inventory.getItems().stream().filter(item -> item.getMaterial() == ItemMaterials.MATERIAL_GOLD)
                .findAny().orElseThrow(RuntimeException::new);

        assertTrue(mod.action(action, creature, goldLump, mod.getActionId(), 0));
        assertEquals(number, contract.getItemCount());
        assertEquals(number, inventory.getItemCount());
        assertTrue(contract.getItems().stream().allMatch(item -> item.getMaterial() == ItemMaterials.MATERIAL_GOLD));
        assertTrue(inventory.getItems().stream().allMatch(item -> item.getMaterial() == ItemMaterials.MATERIAL_IRON));
    }

    @Test
    void testPackItemStackInsidePile() {
        pile.getItems().clear();

        Item notLump = new Item(ItemList.acorn);
        notLump.setName("acorn");
        pile.insertItem(notLump);

        for (int i = 0; i < 10; i++) {
            Item item = new Item(ItemList.ironBar);
            item.setName("lump");
            pile.insertItem(item);
        }

        Item lump = new Item(ItemList.ironBar);
        lump.setName("lump");
        pile.insertItem(lump);

        assertTrue(mod.action(action, creature, lump, mod.getActionId(), 0));
        assertEquals(11, contract.getItemCount());
        assertFalse(contract.contains(notLump));
        assertEquals(1, pile.getItemCount());
        assertTrue(pile.contains(notLump));
    }

    // checkTake

    private void testPackingBlocked(String messageFragment) {
        assertTrue(mod.action(action, creature, itemToPack, mod.getActionId(), 0));
        assertTrue(creature.getCommunicator().getLastMessage().contains(messageFragment));
        assertFalse(contract.getItems().contains(itemToPack));
    }

    private void testPackingNotBlocked() {
        assertTrue(mod.action(action, creature, itemToPack, mod.getActionId(), 0));
        assertTrue(creature.getCommunicator().getLastMessage().contains("take the item"));
        assertTrue(contract.getItems().contains(itemToPack));
    }

    @Test
    void testItemInUse() {
        itemToPack.busy = true;
        testPackingBlocked("in use");
    }

    @Test
    void testOwnedBySomeoneElseNotAllowed() {
        itemToPack.setOwnerId(creature.getWurmId() + 1);
        testPackingBlocked("not own the");
    }

    @Test
    void testCoinsNotAllowed() {
        itemToPack.coin = true;
        testPackingBlocked("cannot pack coins");
    }

    @Test
    void testUnreachableNotAllowed() {
        itemToPack.mailed = true;
        testPackingBlocked("can't reach");
    }

    @Test
    void testLiquidsNotAllowed() {
        itemToPack.liquid = true;
        testPackingBlocked("pour");
    }

    @Test
    void testLiquidsInContainersIsAllowed() {
        itemToPack.hollow = true;
        Item water = new Item(ItemList.water);
        water.liquid = true;
        itemToPack.insertItem(water);
        testPackingNotBlocked();
    }

    @Test
    void testFilledBulkContainerAndTentNotAllowed() {
        itemToPack.insertItem(new Item(ItemList.acorn));

        itemToPack.setTemplateId(ItemList.bulkContainer);
        testPackingBlocked("too heavy");

        itemToPack.setTemplateId(ItemList.tent);
        testPackingBlocked("too heavy");
    }

    @Test
    void testBulkItemsNotAllowed() {
        itemToPack.setTemplateId(ItemList.bulkItem);
        testPackingBlocked("cannot pack");
    }

    @Test
    void testHitchedTentsNotAllowed() {
        itemToPack.setTemplateId(ItemList.tent);
        Vehicles.createVehicle(itemToPack).addDragger(creature);
        testPackingBlocked("hitched");
    }

    @Test
    void testBlockedItemsNotAllowed() {
        Blocking.blocked = true;
        testPackingBlocked("through the wall");
    }

    @Test
    void testItemTooFarAway() {
        creature.withinDistance = false;
        testPackingBlocked("too far away");
    }

    @Test
    void testSameVehicleDoesNotSayItemTooFarAway() {
        creature.withinDistance = false;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.insertItem(itemToPack);
        creature.vehicle = vehicle.getWurmId();
        testPackingNotBlocked();
    }

    @Test
    void testLootingNotAllowed() {
        MethodsItems.isLootable = false;
        testPackingBlocked("may not loot");
    }

    @Test
    void testWatchedVehicleBlockedIfDraggedByOther() {
        MethodsItems.mayUseInventory = false;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.draggable = true;
        itemToPack.lastOwner = -10;
        vehicle.insertItem(itemToPack);
        vehicle.isDragged = true;
        testPackingBlocked("being watched too closely");
    }

    @Test
    void testNotWatchedVehicleHasPermission() {
        MethodsItems.mayUseInventory = true;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.draggable = true;
        vehicle.insertItem(itemToPack);
        testPackingNotBlocked();
    }

    @Test
    void testNotWatchedVehicleTargetLastOwnerIsPacker() {
        MethodsItems.mayUseInventory = false;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.draggable = true;
        vehicle.insertItem(itemToPack);
        itemToPack.lastOwner = creature.getWurmId();
        testPackingNotBlocked();
    }

    @Test
    void testNotWatchedVehicleIfDragged() {
        MethodsItems.mayUseInventory = false;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.draggable = true;
        vehicle.isDragged = true;
        creature.setDraggedItem(vehicle);
        vehicle.insertItem(itemToPack);
        testPackingNotBlocked();
    }

    @Test
    void testWatchedVehicleBlockedIfDraggingOther() {
        MethodsItems.mayUseInventory = false;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.draggable = true;
        vehicle.isDragged = true;
        vehicle.insertItem(itemToPack);
        Item otherVehicle = new Item(ItemList.cartLarge);
        otherVehicle.draggable = true;
        creature.setDraggedItem(otherVehicle);
        testPackingBlocked("being watched too closely");
    }

    @Test
    void testWatchedVehicleBlockedIfLocked() {
        MethodsItems.mayUseInventory = false;
        Item vehicle = new Item(ItemList.cartLarge);
        vehicle.draggable = true;
        vehicle.lock = 100;
        vehicle.insertItem(itemToPack);
        testPackingBlocked("being watched too closely");
    }

    @Test
    void testCartsArePackable() {
        itemToPack.setTemplateId(ItemList.cartLarge);
        testPackingNotBlocked();
    }

    @Test
    void testBoatsNotPackable() {
        itemToPack.setTemplateId(ItemList.boatRowing);
        testPackingBlocked("not be delivered");
    }

    @Test
    void testStealingNotAllowed() {
        MethodsItems.isStealing = true;
        testPackingBlocked("steal");
    }

    @Test
    void testNotBlockItemOwnedByContractUserOrNobody() {
        pile.setOwnerId(creature.getWurmId());
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
        contract.getItems().clear();
        pile.setOwnerId(-10);
        assertTrue(mod.action(action, creature, pile, mod.getActionId(), 0));
    }

    private Village setUpVillage() {
        return new Village();
    }

    @Test
    void testBlockIfDoesNotHaveVillagePermissionToPickup() {
        Village village = setUpVillage();
        village.getRoleFor(creature).setMayPickup(false);
        testPackingBlocked("not have permission");
    }

    @Test
    void testNotBlockIfDoesHaveVillagePermissionToPickup() {
        Village village = setUpVillage();
        village.getRoleFor(creature).setMayPickup(true);
        testPackingNotBlocked();
    }

    @Test
    void testNotBlockPackingItemInInventoryWhenInAnotherVillage() {
        Village village = setUpVillage();
        creature.getInventory().insertItem(itemToPack);
        village.getRoleFor(creature).setMayPickup(false);

        testPackingNotBlocked();
    }

    @Test
    void testBlockIfDoesNotHaveVillagePermissionToPickupPlanted() {
        Village village = setUpVillage();
        village.getRoleFor(creature).setMayPickup(false);
        village.getRoleFor(creature).setMayPickupPlanted(false);
        ItemBehaviour.signManipulation = false;
        itemToPack.setIsPlanted(true);
        itemToPack.noTake = true;
        testPackingBlocked("not have permission");

    }

    @Test
    void testNotBlockIfDoesHaveVillagePermissionToPickupPlanted() {
        Village village = setUpVillage();
        village.getRoleFor(creature).setMayPickupPlanted(true);
        itemToPack.setIsPlanted(true);
        ItemBehaviour.signManipulation = true;
        itemToPack.lastOwner = creature.getWurmId();
        itemToPack.noTake = true;
        testPackingNotBlocked();
    }

    private void trueCheck() {
        assertNotNull(mod.getBehavioursFor(creature, contract, pile));
    }

    private void falseCheck() {
        assertNull(mod.getBehavioursFor(creature, contract, pile));
    }

    private void falseTrueCheck() {
        falseCheck();
        trueCheck();
    }

    @Test
    void testManyBlockOptions() {
        assert contract.getItemCount() == 0;
        pile = spy(pile);
        when(pile.getBridgeId()).thenReturn(creature.getBridgeId());
        Village village = setUpVillage();
        village.getRoleFor(creature).setMayPickup(true);

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
        when(pile.isEmpty(anyBoolean())).thenReturn(false, true);
        falseTrueCheck();

        when(pile.canBeDropped(anyBoolean())).thenReturn(false, true);
        falseTrueCheck();

        MethodsItems.isStealing = true;
        falseCheck();

        MethodsItems.isStealing = false;
        trueCheck();
    }

    @Test
    void testCannotTakeUniqueItems() {
        itemToPack.unique = true;
        testPackingBlocked("special");
    }

    @Test
    void testCannotPackItemsPlantedByOther() {
        ItemBehaviour.signManipulation = false;
        itemToPack.setIsPlanted(true);
        itemToPack.lastOwner = creature.getWurmId() + 1;
        testPackingBlocked("planted by");
    }

    @Test
    void testCanPackItemsPlantedByPacker() {
        ItemBehaviour.signManipulation = true;
        itemToPack.setIsPlanted(true);
        itemToPack.lastOwner = creature.getWurmId();
        testPackingNotBlocked();
    }

    // actuallyTake - Not tested directly.

    @Test
    void testItemsRemovedFromZone() {
        Zone zone = Zones.getZone((int)itemToPack.getPosX() >> 2, (int)itemToPack.getPosY() >> 2, true);
        zone.addItem(itemToPack);

        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertFalse(zone.items.contains(itemToPack));
    }

    @Test
    void testDraggingStopped() {
        itemToPack.setTemplateId(ItemList.cartLarge);
        Items.startDragging(creature, itemToPack);

        mod.action(action, creature, pile, mod.getActionId(), 0);

        assertNull(creature.getDraggedItem());
    }

    @Test
    void testPlantedItemNoLongerPlantedWhenPacked() {
        ItemBehaviour.signManipulation = true;
        itemToPack.setIsPlanted(true);
        itemToPack.setTemplateId(ItemList.forge);

        assert itemToPack.isPlanted();

        mod.action(action, creature, itemToPack, mod.getActionId(), 0);

        assertFalse(itemToPack.isPlanted());
    }

    @Test
    void testWatchersAreUpdated() throws NoSuchCreatureException {
        Creature watcher = new Creature();
        watcher.currentTile = new VolaTile(0);
        itemToPack.putItemInfrontof(watcher);
        itemToPack.addWatcher(0, watcher);

        mod.action(action, creature, itemToPack, mod.getActionId(), 0);

        assertEquals(0, itemToPack.getWatchers().length);
    }
}
