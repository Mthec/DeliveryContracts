package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.economy.Economy;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.Blocking;
import com.wurmonline.server.zones.VolaTile;
import com.wurmonline.server.zones.Zones;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

public class ActionBehaviourTest {
    protected Creature creature;
    int contractTemplateId = 12345;
    protected Item contract;
    protected Item itemToPack;
    Item pile;
    Item waystone;
    Item villageToken;
    Action action;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field t = DeliveryContractsMod.class.getDeclaredField("templateId");
        t.setAccessible(true);
        t.set(null, contractTemplateId);

        Blocking.blocked = false;
        Creatures.reset();
        Items.reset();
        Vehicles.reset();
        MethodsItems.reset();
        Zones.reset();
        Economy.reset();

        creature = new Creature();
        creature.currentTile = new VolaTile(0);
        contract = new Item(contractTemplateId);
        contract.hollow = true;
        contract.setName("Contract");
        itemToPack = new Item(ItemList.ironBar);
        pile = new Item(ItemList.itemPile);
        pile.insertItem(itemToPack);
        pile.insertItem(new Item(ItemList.ironBar));
        waystone = new Item(ItemList.waystone);
        waystone.setPlanted(true);
        villageToken = new Item(ItemList.villageToken);
        action = new Action(contract.getWurmId());

        Items.addItems(contract, pile, waystone, villageToken);
    }
}
