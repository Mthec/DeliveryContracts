package com.wurmonline.server.items;

import com.wurmonline.server.behaviours.ActionBehaviourTest;
import com.wurmonline.server.behaviours.MethodsCreatures;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.TradeHandler;
import com.wurmonline.server.economy.Economy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TradingTests extends ActionBehaviourTest {

    @Test
    void testContentsRemainWhenTrading() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Creature owner = creature;
        Creature merchant = new Creature();
        merchant.player = false;
        Economy.addTrader(merchant);
        when(Economy.getEconomy().getShop(merchant).isPersonal()).thenReturn(true);
        when(Economy.getEconomy().getShop(merchant).getOwnerId()).thenReturn(owner.getWurmId());
        Creature player = new Creature();

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
}
