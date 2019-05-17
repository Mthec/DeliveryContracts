package mod.wurmunlimited.delivery;

import com.wurmonline.server.Items;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.behaviours.Actions;
import com.wurmonline.server.behaviours.PackContractAction;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.TradingWindow;
import com.wurmonline.server.players.Player;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class DeliveryContractsModTests {

    private static final int contractTemplateId = 12356;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field t = DeliveryContractsMod.class.getDeclaredField("templateId");
        t.setAccessible(true);
        t.set(null, contractTemplateId);
    }

    @Test
    void testCreateShop() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        properties.setProperty("contracts_on_traders", "true");
        deliveryContractsMod.configure(properties);
        InvocationHandler handler = deliveryContractsMod::createShop;
        Method method = mock(Method.class);

        Creature trader = new Creature();

        Object[] args = new Object[] {trader};
        assertNull(handler.invoke(null, method, args));
        assertTrue(trader.getInventory().getItems().stream().anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        verify(method, times(1)).invoke(null, args);
    }

    @Test
    void testCreateShopNotAddedIfContractsDisabled() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        properties.setProperty("contracts_on_traders", "false");
        deliveryContractsMod.configure(properties);
        InvocationHandler handler = deliveryContractsMod::createShop;
        Method method = mock(Method.class);

        Creature trader = new Creature();

        Object[] args = new Object[] {trader};
        assertNull(handler.invoke(null, method, args));
        assertFalse(trader.getInventory().getItems().stream().anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        verify(method, times(1)).invoke(null, args);
    }

    @Test
    void testConfigureContractPrice() throws NoSuchFieldException, IllegalAccessException {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        Field price = DeliveryContractsMod.class.getDeclaredField("contractPrice");
        price.setAccessible(true);
        for (int i = 0; i < 1000; i++) {
            properties.setProperty("contract_price_in_irons", String.valueOf(i));
            deliveryContractsMod.configure(properties);
            assertEquals(i, price.get(deliveryContractsMod));
        }
    }

    @Test
    void testConfigureContractPriceInvalidValue() throws NoSuchFieldException, IllegalAccessException {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Field price = DeliveryContractsMod.class.getDeclaredField("contractPrice");
        price.setAccessible(true);
        int defaultPrice = price.getInt(deliveryContractsMod);
        Properties properties = new Properties();

        properties.setProperty("contract_price_in_irons", String.valueOf(1.0f));
        deliveryContractsMod.configure(properties);
        assertEquals(defaultPrice, price.get(deliveryContractsMod));

        properties.setProperty("contract_price_in_irons", "name");
        deliveryContractsMod.configure(properties);
        assertEquals(defaultPrice, price.get(deliveryContractsMod));

        properties.setProperty("contract_price_in_irons", String.valueOf(-100));
        deliveryContractsMod.configure(properties);
        assertEquals(defaultPrice, price.get(deliveryContractsMod));
    }

    private void createTraders() {
        Creatures.reset();
        Economy.reset();
        Creature trader1 = new Creature();
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.setOwnerId(trader1.getWurmId());
        trader1.getInventory().insertItem(contract);
        Creatures.getInstance().addCreature(trader1);
        Economy.reset();
        Economy.addTrader(trader1);
        Creature trader2 = new Creature();
        Creatures.getInstance().addCreature(trader2);
        Economy.addTrader(trader2);
    }

    @Test
    void testUpdateTradersTrueContractsOnTrue() {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        properties.setProperty("update_traders", "true");
        properties.setProperty("contracts_on_traders", "true");
        deliveryContractsMod.configure(properties);

        createTraders();
        deliveryContractsMod.onServerStarted();

        for (Shop shop : Economy.getTraders()) {
            assertTrue(Creatures.getInstance().getCreatureOrNull(shop.getWurmId()).getInventory().getItems().stream()
                               .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        }
    }

    @Test
    void testUpdateTradersFalseContractsOnTrue() {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        properties.setProperty("update_traders", "false");
        properties.setProperty("contracts_on_traders", "true");
        deliveryContractsMod.configure(properties);

        createTraders();
        deliveryContractsMod.onServerStarted();

        Shop[] shops = Economy.getTraders();
        if (Creatures.getInstance().getCreatureOrNull(shops[0].getWurmId()).getInventory().getItems().stream()
                    .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId())) {
            assertFalse(Creatures.getInstance().getCreatureOrNull(shops[1].getWurmId()).getInventory().getItems().stream()
                               .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        } else {
            assertTrue(Creatures.getInstance().getCreatureOrNull(shops[1].getWurmId()).getInventory().getItems().stream()
                               .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        }
    }

    @Test
    void testUpdateTradersTrueContractsOnFalse() {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        properties.setProperty("update_traders", "true");
        properties.setProperty("contracts_on_traders", "false");
        deliveryContractsMod.configure(properties);

        createTraders();
        deliveryContractsMod.onServerStarted();

        for (Shop shop : Economy.getTraders()) {
            assertTrue(Creatures.getInstance().getCreatureOrNull(shop.getWurmId()).getInventory().getItems().stream()
                               .noneMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        }
    }

    @Test
    void testUpdateTradersFalseContractsOnFalse() {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Properties properties = new Properties();
        properties.setProperty("update_traders", "false");
        properties.setProperty("contracts_on_traders", "false");
        deliveryContractsMod.configure(properties);

        createTraders();
        deliveryContractsMod.onServerStarted();

        Shop[] shops = Economy.getTraders();
        if (Creatures.getInstance().getCreatureOrNull(shops[0].getWurmId()).getInventory().getItems().stream()
                    .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId())) {
            assertFalse(Creatures.getInstance().getCreatureOrNull(shops[1].getWurmId()).getInventory().getItems().stream()
                                .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        } else {
            assertTrue(Creatures.getInstance().getCreatureOrNull(shops[1].getWurmId()).getInventory().getItems().stream()
                               .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        }
    }

    @Test
    void testContractIsFullPriceIfEmpty() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::isFullPrice;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        assert contract.getItemCount() == 0;

        Object[] args = new Object[0];
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testContractIsNotFullPriceIfNotEmpty() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::isFullPrice;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.insertItem(new Item(ItemList.acorn));

        Object[] args = new Object[0];
        assertFalse((Boolean)handler.invoke(contract, method, args));
        verify(method, never()).invoke(contract, args);
    }

    @Test
    void testMayAddToInventoryIfNotInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::mayAddFromInventory;
        Method method = mock(Method.class);

        Item contract = new Item(ItemList.backPack);
        Item item = new Item(ItemList.acorn);
        contract.insertItem(item);

        Object[] args = new Object[] { new Player(), item };
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testMayNotAddToInventoryIfInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::mayAddFromInventory;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item item = new Item(ItemList.acorn);
        contract.insertItem(item);

        Object[] args = new Object[] { new Player(), item };
        assertFalse((Boolean)handler.invoke(contract, method, args));
        verify(method, never()).invoke(contract, args);
    }

    @Test
    void testMayMoveToItemIfNotInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::moveToItem;
        Method method = mock(Method.class);

        Item contract = new Item(ItemList.backPack);
        Item item = new Item(ItemList.acorn);
        contract.insertItem(item);

        Object[] args = new Object[] { new Player(), item.getWurmId(), false };
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testMayNotMoveToItemIfInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::moveToItem;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item item = new Item(ItemList.acorn);
        contract.insertItem(item);

        Object[] args = new Object[] { new Player(), item.getWurmId(), false };
        assertFalse((Boolean)handler.invoke(item, method, args));
        verify(method, never()).invoke(item, args);
    }

    @Test
    void testMayNotMoveToItemIfInItemThatIsInAContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::moveToItem;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item containerInContract = new Item(ItemList.backPack);
        contract.insertItem(containerInContract);
        Item containerInContainerInContract = new Item(ItemList.backPack);
        containerInContract.insertItem(containerInContainerInContract);
        Item item = new Item(ItemList.acorn);
        containerInContainerInContract.insertItem(item);

        Object[] args = new Object[] { new Player(), item.getWurmId(), false };
        assertFalse((Boolean)handler.invoke(item, method, args));
        verify(method, never()).invoke(item, args);
    }

    @Test
    void testMayNotMoveToItemIfDestinationIsInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::moveToItem;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item containerInContract = new Item(ItemList.backPack);
        containerInContract.hollow = true;
        contract.insertItem(containerInContract);
        Item item = new Item(ItemList.acorn);

        Object[] args = new Object[] { new Player(), item.getWurmId(), false };
        assertFalse((Boolean)handler.invoke(containerInContract, method, args));
        verify(method, never()).invoke(containerInContract, args);
    }

    private int changeOwner(Object[] args, int parentTemplateId, int weight) throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::setOwner;
        Method method = mock(Method.class);
        Creature player = new Player();

        final int[] weightAdded = {0};
        Item contract = new Item(parentTemplateId);
        Item item = new Item(ItemList.dirtPile);
        item.setWeight(weight);
        contract.insertItem(item);
        player.getInventory().insertItem(contract);

        InvocationHandler addCarriedWeight = deliveryContractsMod::addCarriedWeight;
        Method addWeight = mock(Method.class);
        when(addWeight.invoke(player, weight)).then((Answer<Void>)invocation -> {
            weightAdded[0] += invocation.<Integer>getArgument(1);
            return null;
        });
        when(method.invoke(any(), any())).then((Answer<Void>)invocation -> {
            addCarriedWeight.invoke(player, addWeight, new Object[] {weight});
            return null;
        });

        if (args[0] instanceof Long)
            args[0] = player.getWurmId();
        assertNull(handler.invoke(item, method, args));
        verify(method, times(1)).invoke(item, args);
        return weightAdded[0];
    }

    private int setOwner(int parentTemplateId, int weight) throws Throwable {
        return changeOwner(new Object[] { -10L, 1L, false }, parentTemplateId, weight);
    }

    private int setOwnerStuff(ItemTemplate template, int weight) throws Throwable {
        return changeOwner(new Object[] { template }, template.getTemplateId(), weight);
    }

    // TODO - Re-enable.
//    @Test
//    void testSetOwnerBlocksContractContentsWeightBeingAddedToCreature() throws Throwable {
//        assertEquals(0, setOwner(DeliveryContractsMod.getTemplateId(), 1000));
//    }

    @Test
    void testSetOwnerAllowsNotContractContentsWeightBeingAddedToCreature() throws Throwable {
        int weight = 1000;
        assertEquals(weight, setOwner(ItemList.backPack, weight));
    }

//    @Test
//    void testSetOwnerStuffBlocksContractContentsWeightBeingAddedToCreature() throws Throwable {
//        assertEquals(0, setOwnerStuff(new ItemTemplate(DeliveryContractsMod.getTemplateId(), "contract"), 1000));
//    }

    @Test
    void testSetOwnerStuffAllowsNotContractContentsWeightBeingAddedToCreature() throws Throwable {
        int weight = 1000;
        assertEquals(weight, setOwnerStuff(new ItemTemplate(ItemList.backPack, "backpack"), weight));
    }

//    @Test
//    void testAddCarriedWeightBlocks() throws Throwable {
//        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
//        Creature player = new Player();
//        int weight = 100000;
//
//        InvocationHandler handler = deliveryContractsMod::addCarriedWeight;
//        Method method = mock(Method.class);
//        Object[] args = new Object[] { weight };
//
//        DeliveryContractsMod.addWeightToBlock(player, weight);
//
//        assertNull(handler.invoke(player, method, args));
//        verify(method, never()).invoke(player, args);
//
//        assertNull(handler.invoke(player, method, args));
//        verify(method, times(1)).invoke(player, args);
//    }
//
//    @Test
//    void testRemoveCarriedWeightBlocks() throws Throwable {
//        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
//        Creature player = new Player();
//        int weight = 100000;
//
//        InvocationHandler handler = deliveryContractsMod::removeCarriedWeight;
//        Method method = mock(Method.class);
//        Object[] args = new Object[] { weight };
//
//        DeliveryContractsMod.addWeightToBlock(player, weight);
//
//        assertTrue((Boolean)handler.invoke(player, method, args));
//        verify(method, never()).invoke(player, args);
//
//        assertNull(handler.invoke(player, method, args));
//        verify(method, times(1)).invoke(player, args);
//    }

    @Test
    void testGetItemsAsArrayReturnsEmptyIfContractWhenTraded() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::getItemsAsArray;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.tradeWindow = mock(TradingWindow.class);
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[0];
        Object[] items = (Object[])handler.invoke(contract, method, args);
        assertEquals(0, items.length);
        verify(method, never()).invoke(contract, args);
    }

    @Test
    void testGetItemsAsArrayReturnsItemsIfContractButNotTraded() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::getItemsAsArray;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[0];
        Object[] items = (Object[])handler.invoke(contract, method, args);
        assertNull(items);
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testGetItemsAsArrayReturnsItemsIfNotContractWhenTraded() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::getItemsAsArray;
        Method method = mock(Method.class);

        Item contract = new Item(ItemList.backPack);
        contract.tradeWindow = mock(TradingWindow.class);
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[0];
        Object[] items = (Object[])handler.invoke(contract, method, args);
        assertNull(items);
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testIsEmptyReturnsTrueIfContractWhenTraded() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::isEmpty;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.tradeWindow = mock(TradingWindow.class);
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[] { true };
        assertTrue((Boolean)handler.invoke(contract, method, args));
        verify(method, never()).invoke(contract, args);
    }

    @Test
    void testIsEmptyReturnsNormalIfContractWhenTradedButArgs0IsFalse() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::isEmpty;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.tradeWindow = mock(TradingWindow.class);
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[] { false };
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testIsEmptyReturnsNormalIfContractButNotTraded() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::isEmpty;
        Method method = mock(Method.class);

        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[] { true };
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testIsEmptyReturnsNormalIfNotContractWhenTraded() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::isEmpty;
        Method method = mock(Method.class);

        Item contract = new Item(ItemList.backPack);
        contract.tradeWindow = mock(TradingWindow.class);
        int numberOfItems = 25;
        for (int i = 0; i < numberOfItems; i++) {
            Item item = new Item(ItemList.dirtPile);
            contract.insertItem(item);
        }

        Object[] args = new Object[] { true };
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testBlockBehavioursForItemsInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Creature player = new Player();
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item itemInContract = new Item(ItemList.ironBar);
        contract.insertItem(itemInContract);

        InvocationHandler handler = deliveryContractsMod::getBehavioursFor;
        Method method = mock(Method.class);
        Object[] args = new Object[] { player, itemInContract };

        @SuppressWarnings("unchecked")
        List<ActionEntry> entries = (List<ActionEntry>)handler.invoke(itemInContract, method, args);
        assertNotNull(entries);
        assertEquals(0, entries.size());
        verify(method, never()).invoke(itemInContract, args);
    }

    @Test
    void testDoesNotBlockBehavioursForItemsNotInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Creature player = new Player();
        Item contract = new Item(ItemList.backPack);
        Item itemInContract = new Item(ItemList.ironBar);
        contract.insertItem(itemInContract);

        InvocationHandler handler = deliveryContractsMod::getBehavioursFor;
        Method method = mock(Method.class);
        Object[] args = new Object[] { player, itemInContract };

        @SuppressWarnings("unchecked")
        List<ActionEntry> entries = (List<ActionEntry>)handler.invoke(itemInContract, method, args);
        assertNull(entries);
        verify(method, times(1)).invoke(itemInContract, args);
    }

    @Test
    void testBlockBehavioursForAlternateGetBehavioursForItemsInContract() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Creature player = new Player();
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item itemInContract = new Item(ItemList.ironBar);
        contract.insertItem(itemInContract);

        InvocationHandler handler = deliveryContractsMod::getBehavioursFor;
        Method method = mock(Method.class);
        Object[] args = new Object[] { player, contract, itemInContract };

        @SuppressWarnings("unchecked")
        List<ActionEntry> entries = (List<ActionEntry>)handler.invoke(itemInContract, method, args);
        assertNotNull(entries);
        assertEquals(0, entries.size());
        verify(method, never()).invoke(itemInContract, args);
    }

    @Test
    void testGetFullWeightReturnsZeroForContract() throws Throwable {
        Item contract = new Item(DeliveryContractsMod.getTemplateId());

        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::getFullWeight;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        assertEquals(0, handler.invoke(contract, method, args));
        verify(method, never()).invoke(contract, args);
    }

    @Test
    void testGetFullWeightReturnsNormalForNotContract() throws Throwable {
        Item contract = new Item(DeliveryContractsMod.getTemplateId() + 1);

        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        InvocationHandler handler = deliveryContractsMod::getFullWeight;
        Method method = mock(Method.class);
        Object[] args = new Object[0];

        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }

    @Test
    void testBlockUnintendedMultiplePackingIntoContentsBlocked() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item pile = new Item(ItemList.itemPile);
        Item dirt1 = new Item(ItemList.dirtPile);
        Item dirt2 = new Item(ItemList.dirtPile);
        Item dirt3 = new Item(ItemList.dirtPile);
        pile.insertItem(dirt1);
        pile.insertItem(dirt2);
        pile.insertItem(dirt3);
        short actionId = 12323;
        PackContractAction packAction = ReflectionUtil.callPrivateConstructor(PackContractAction.class.getDeclaredConstructor(short.class), actionId);
        PackContractAction.itemCap = 1000;
        ReflectionUtil.setPrivateField(deliveryContractsMod, DeliveryContractsMod.class.getDeclaredField("packActionId"), actionId);
        Creature player = new Player();

        InvocationHandler handler = deliveryContractsMod::behaviourDispatcher;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).then((Answer<Void>)i -> {
            Action action = mock(Action.class);
            when(action.getSubjectId()).thenReturn(i.getArgument(3));
            packAction.action(action, player, Items.getItem(i.getArgument(4)), actionId, 0);
            return null;
        });
        Object[] args1 = new Object[] { player, mock(Communicator.class), contract.getWurmId(), dirt1.getWurmId(), actionId };
        Object[] args2 = new Object[] { player, mock(Communicator.class), contract.getWurmId(), dirt2.getWurmId(), actionId };
        Object[] args3 = new Object[] { player, mock(Communicator.class), contract.getWurmId(), dirt3.getWurmId(), actionId };

        assertNull(handler.invoke(contract, method, args1));
        verify(method, times(1)).invoke(contract, args1);
        assertNull(handler.invoke(contract, method, args2));
        verify(method, never()).invoke(contract, args2);
        assertNull(handler.invoke(contract, method, args3));
        verify(method, never()).invoke(contract, args3);
    }

    @Test
    void testBlockNormalMultipleActionNotBlocked() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item source = new Item(ItemList.wandGM);
        Item pile = new Item(ItemList.itemPile);
        Item dirt1 = new Item(ItemList.dirtPile);
        Item dirt2 = new Item(ItemList.dirtPile);
        Item dirt3 = new Item(ItemList.dirtPile);
        pile.insertItem(dirt1);
        pile.insertItem(dirt2);
        pile.insertItem(dirt3);
        short actionId = 7;
        Creature player = new Player();

        InvocationHandler handler = deliveryContractsMod::behaviourDispatcher;
        Method method = mock(Method.class);
        Object[] args1 = new Object[] { player, mock(Communicator.class), source.getWurmId(), dirt1.getWurmId(), actionId };
        Object[] args2 = new Object[] { player, mock(Communicator.class), source.getWurmId(), dirt2.getWurmId(), actionId };
        Object[] args3 = new Object[] { player, mock(Communicator.class), source.getWurmId(), dirt3.getWurmId(), actionId };

        assertNull(handler.invoke(source, method, args1));
        verify(method, times(1)).invoke(source, args1);
        assertNull(handler.invoke(source, method, args2));
        verify(method, times(1)).invoke(source, args2);
        assertNull(handler.invoke(source, method, args3));
        verify(method, times(1)).invoke(source, args3);
    }

    @Test
    void testBehaviourDispatcherAllowsPackedItemsToBeDestroyed() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item source = new Item(ItemList.wandGM);
        Item dirt1 = new Item(ItemList.dirtPile);
        dirt1.setMailed(true);
        short actionId = Actions.DESTROY;
        Creature player = new Player();
        player.power = 2;

        InvocationHandler handler = deliveryContractsMod::behaviourDispatcher;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).then((Answer<Void>)i -> {
            Item target = Items.getItem(i.getArgument(4));
            if (!target.isMailed())
                Items.destroyItem(target.getWurmId());
            return null;
        });
        Object[] args1 = new Object[] { player, mock(Communicator.class), source.getWurmId(), dirt1.getWurmId(), actionId };

        assertNull(handler.invoke(source, method, args1));
        verify(method, times(1)).invoke(source, args1);
        assertTrue(Items.wasDestroyed(dirt1));
    }

    @Test
    void testBehaviourDispatcherBlocksPackedItemsBeingDestroyedByNonGM() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item source = new Item(ItemList.wandGM);
        Item dirt1 = new Item(ItemList.dirtPile);
        dirt1.setMailed(true);
        short actionId = Actions.DESTROY;
        Creature player = new Player();
        player.power = 0;

        InvocationHandler handler = deliveryContractsMod::behaviourDispatcher;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).then((Answer<Void>)i -> {
            Item target = Items.getItem(i.getArgument(4));
            if (!target.isMailed())
                Items.destroyItem(target.getWurmId());
            return null;
        });
        Object[] args1 = new Object[] { player, mock(Communicator.class), source.getWurmId(), dirt1.getWurmId(), actionId };

        assertNull(handler.invoke(source, method, args1));
        verify(method, times(1)).invoke(source, args1);
        assertFalse(Items.wasDestroyed(dirt1));
    }

    @Test
    void testDestroyItemDestroysContents() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        Item contents = new Item(ItemList.dirtPile);
        contract.insertItem(contents);

        InvocationHandler handler = deliveryContractsMod::destroyItem;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).then((Answer<Void>)i -> {
            Items.destroyItem(((Item)i.getArgument(0)).getWurmId());
            return null;
        });
        Object[] args = new Object[] { contract.getWurmId(), true, true };

        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
        assertTrue(Items.wasDestroyed(contract));
        assertTrue(Items.wasDestroyed(contents));
    }

    @Test
    void testDestroyItemDoesNotOverrideNormalBehaviourForOtherItems() throws Throwable {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item contents = new Item(ItemList.bowlPottery);
        Item subContents = new Item(ItemList.acorn);
        contents.insertItem(subContents);

        InvocationHandler handler = deliveryContractsMod::destroyItem;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).then((Answer<Void>)i -> {
            Items.destroyItem(((Item)i.getArgument(0)).getWurmId());
            return null;
        });
        Object[] args = new Object[] { contents.getWurmId(), true, true };

        assertNull(handler.invoke(contents, method, args));
        verify(method, times(1)).invoke(contents, args);
        assertTrue(Items.wasDestroyed(contents));
        assertFalse(Items.wasDestroyed(subContents));
    }

    private class FakeException extends Exception {}

    @Test
    void testBehaviourDispatcherExceptionsNotBlocked() throws InvocationTargetException, IllegalAccessException {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item source = new Item(ItemList.wandGM);
        Item dirt1 = new Item(ItemList.dirtPile);
        short actionId = 7;
        Creature player = new Player();

        InvocationHandler handler = deliveryContractsMod::behaviourDispatcher;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenThrow(new InvocationTargetException(new FakeException()));
        Object[] args1 = new Object[] { player, mock(Communicator.class), source.getWurmId(), dirt1.getWurmId(), actionId };

        assertThrows(FakeException.class, () -> handler.invoke(source, method, args1));
    }

    @Test
    void testMoveToItemExceptionsNotBlocked() throws InvocationTargetException, IllegalAccessException {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Item source = new Item(ItemList.wandGM);
        Creature player = new Player();

        InvocationHandler handler = deliveryContractsMod::moveToItem;
        Method method = mock(Method.class);
        when(method.invoke(any(), any())).thenThrow(new InvocationTargetException(new FakeException()));
        Object[] args1 = new Object[] { player, 1L, true };

        assertThrows(FakeException.class, () -> handler.invoke(source, method, args1));
    }

    @Test
    void testCleanupCommandOnlyByGM() {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Creature player = new Player();
        player.power = 0;
        Communicator comm = player.getCommunicator();
        Items.reset();
        int num = 10;
        for (int i = 0; i < num; i++) {
            Item item = new Item(ItemList.dirtPile);
            item.mailed = true;
        }

        assertEquals(MessagePolicy.PASS, deliveryContractsMod.onPlayerMessage(comm, "#doCleanup", ""));
        assertTrue(Arrays.stream(Items.getAllItems()).noneMatch(i -> i.getTemplateId() == ItemList.dirtPile && Items.wasDestroyed(i)));
    }

    @Test
    void testCleanupCommandNotCalledIfDifferentCommand() {
        DeliveryContractsMod deliveryContractsMod = new DeliveryContractsMod();
        Creature player = new Player();
        player.power = 0;
        Communicator comm = player.getCommunicator();
        Items.reset();
        int num = 10;
        for (int i = 0; i < num; i++) {
            Item item = new Item(ItemList.dirtPile);
            item.mailed = true;
        }

        assertEquals(MessagePolicy.PASS, deliveryContractsMod.onPlayerMessage(comm, "#dontDoCleanup", ""));
        assertTrue(Arrays.stream(Items.getAllItems()).noneMatch(i -> i.getTemplateId() == ItemList.dirtPile && Items.wasDestroyed(i)));
    }
}
