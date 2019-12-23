package com.wurmonline.server.items;

import com.wurmonline.server.behaviours.ActionBehaviourTest;
import com.wurmonline.server.behaviours.MethodsCreatures;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.players.Player;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradingTests extends ActionBehaviourTest {

    @Test
    void testContentsRemainWhenTrading() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Creature owner = creature;
        Creature merchant = new Creature();
        owner.player = true;
        Economy.addTrader(merchant);
        when(Economy.getEconomy().getShop(merchant).isPersonal()).thenReturn(true);
        when(Economy.getEconomy().getShop(merchant).getOwnerId()).thenReturn(owner.getWurmId());
        Creature player = new Creature();
        player.player = true;

        Method initiateTrade = MethodsCreatures.class.getDeclaredMethod("initiateTrade", Creature.class, Creature.class);
        initiateTrade.setAccessible(true);
        Method balance = TradeHandler.class.getDeclaredMethod("balance");
        balance.setAccessible(true);

        contract.insertItem(itemToPack);
        contract.insertItem(new Item(ItemList.acorn));
        Set<Item> items = contract.getItems();
        creature.getInventory().insertItem(contract);

        initiateTrade.invoke(null, owner, merchant);
        Trade trade = owner.getTrade();
        trade.getTradingWindow(2).addItem(owner.getInventory().getItems().iterator().next());
        balance.invoke(merchant.getTradeHandler());
        trade.setSatisfied(merchant, true, trade.getCurrentCounter());
        trade.setSatisfied(owner, true, trade.getCurrentCounter());

        assertTrue(merchant.getInventory().contains(contract));
        assertFalse(owner.getInventory().contains(contract));

        initiateTrade.invoke(null, player, merchant);
        trade = player.getTrade();

        Item coin = new Item(ItemList.coinIron);
        coin.coin = true;
        player.getInventory().insertItem(coin);
        trade.getTradingWindow(2).addItem(coin);
        Item contractOnMerchant = trade.getTradingWindow(1).getItems()[0];
        trade.getTradingWindow(1).removeItem(contractOnMerchant);
        trade.getTradingWindow(3).addItem(contractOnMerchant);


        balance.invoke(merchant.getTradeHandler());
        trade.setSatisfied(merchant, true, trade.getCurrentCounter());
        trade.setSatisfied(player, true, trade.getCurrentCounter());

        assertFalse(merchant.getInventory().contains(contract));
        assertTrue(player.getInventory().contains(contract));

        assertEquals(2, contract.getItemCount());
        assertTrue(contract.getItems().containsAll(items));
    }

    @Test
    void testKingsShopUpdatedCorrectly() {
        Creature player = new Player();
        Creature trader = new Creature();
        trader.player = false;
        Economy.reset();
        Economy.addTrader(trader);
        when(Economy.getEconomy().getShop(trader).isPersonal()).thenReturn(false);
        when(Economy.getEconomy().getShop(trader).getOwnerId()).thenReturn(-10L);

        long price = 100;
        Item contract = new Item(DeliveryContractsMod.getTemplateId());
        contract.price = price;
        contract.value = price;
        contract.fullPrice = true;
        contract.noSellBack = true;
        trader.getInventory().insertItem(contract);

        Trade trade = new Trade(player, trader);
        player.setTrade(trade);
        trader.setTrade(trade);
        TradingWindow window = new TradingWindow(trader, player, false, 3, trade);
        window.addItem(contract);
        window.swapOwners();

        verify(Economy.getEconomy().getKingsShop(), times(1)).setMoney(price / 4);
        assertFalse(Economy.getEconomy().boughtByTraders.containsKey(DeliveryContractsMod.getTemplateId()));
        assertEquals(1, (int)Economy.getEconomy().soldByTraders.get(DeliveryContractsMod.getTemplateId()));
        assertEquals(price, (long)Economy.getEconomy().itemsSoldByTraders.get(contract.getName()));

        assertFalse(trader.getInventory().contains(contract));
        assertEquals(1, trader.getInventory().getItemCount());
        assertEquals(DeliveryContractsMod.getTemplateId(), trader.getInventory().getItems().iterator().next().getTemplateId());
        assertTrue(player.getInventory().contains(contract));
    }
}
