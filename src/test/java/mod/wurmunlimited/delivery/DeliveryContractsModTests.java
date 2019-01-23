package mod.wurmunlimited.delivery;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.TradingWindow;
import com.wurmonline.server.players.Player;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

class DeliveryContractsModTests {

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

        Object[] args = new Object[] { new Player(), item };
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

    @Test
    void testSetOwnerBlocksContractContentsWeightBeingAddedToCreature() throws Throwable {
        assertEquals(0, setOwner(DeliveryContractsMod.getTemplateId(), 1000));
    }

    @Test
    void testSetOwnerAllowsNotContractContentsWeightBeingAddedToCreature() throws Throwable {
        int weight = 1000;
        assertEquals(weight, setOwner(ItemList.backPack, weight));
    }

    @Test
    void testSetOwnerStuffBlocksContractContentsWeightBeingAddedToCreature() throws Throwable {
        assertEquals(0, setOwnerStuff(new ItemTemplate(DeliveryContractsMod.getTemplateId()), 1000));
    }

    @Test
    void testSetOwnerStuffAllowsNotContractContentsWeightBeingAddedToCreature() throws Throwable {
        int weight = 1000;
        assertEquals(weight, setOwnerStuff(new ItemTemplate(ItemList.backPack), weight));
    }

    // TODO - Carry weight

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

    // TODO - IsEmpty
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

        Object[] args = new Object[0];
        assertTrue((Boolean)handler.invoke(contract, method, args));
        verify(method, never()).invoke(contract, args);
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

        Object[] args = new Object[0];
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

        Object[] args = new Object[0];
        assertNull(handler.invoke(contract, method, args));
        verify(method, times(1)).invoke(contract, args);
    }
}
