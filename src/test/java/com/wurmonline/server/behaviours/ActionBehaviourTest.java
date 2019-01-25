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
        ItemBehaviour.signManipulation = false;
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
        contract.setName("contract");
        itemToPack = new Item(ItemList.ironBar);
        itemToPack.setName("lump, iron");
        pile = new Item(ItemList.itemPile);
        pile.insertItem(itemToPack);
        Item otherItem = new Item(ItemList.goldBar);
        otherItem.setName("lump, gold");
        pile.insertItem(otherItem);
        waystone = new Item(ItemList.waystone);
        waystone.setIsPlanted(true);
        villageToken = new Item(ItemList.villageToken);
        action = new Action(contract.getWurmId());

        Items.addItems(contract, pile, waystone, villageToken);
    }
}
