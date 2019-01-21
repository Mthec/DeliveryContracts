package mod.wurmunlimited.delivery;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.economy.Shop;
import com.wurmonline.server.items.Item;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        Creature trader1 = new Creature();
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.setOwnerId(trader1.getWurmId());
        trader1.getInventory().insertItem(contract);
        Creatures.getInstance().addCreature(trader1);
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
                    .noneMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId())) {
            assertFalse(Creatures.getInstance().getCreatureOrNull(shops[1].getWurmId()).getInventory().getItems().stream()
                                .anyMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        } else {
            assertTrue(Creatures.getInstance().getCreatureOrNull(shops[1].getWurmId()).getInventory().getItems().stream()
                               .noneMatch(item -> item.getTemplateId() == DeliveryContractsMod.getTemplateId()));
        }
    }
}
