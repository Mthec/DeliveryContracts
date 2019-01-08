package com.wurmonline.server.behaviours;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.structures.Blocking;
import com.wurmonline.server.villages.Village;
import mod.wurmunlimited.delivery.DeliveryContractsMod;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;

public class ActionBehaviourTest {
    Creature creature;
    Village village;
    int contractTemplateId = 12345;
    Item contract;
    Item pile;
    Item waystone;
    Item villageToken;
    Action action;
    public static final int bridgeId = 121;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field t = DeliveryContractsMod.class.getDeclaredField("templateId");
        t.setAccessible(true);
        t.set(null, contractTemplateId);

        Blocking.blocked = false;
        Items.reset();

        creature = new Creature();
        village = new Village();
        creature.setVillageId(village.getId());
        contract = new Item(contractTemplateId);
        pile = new Item(ItemList.itemPile);
        pile.insertItem(new Item(ItemList.ironBar));
        waystone = new Item(ItemList.waystone);
        villageToken = new Item(ItemList.villageToken);
        action = new Action(contract.getWurmId());

        Items.addItems(contract, pile, waystone, villageToken);
    }
}
