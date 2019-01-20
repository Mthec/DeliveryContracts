package com.wurmonline.server;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;

import java.util.*;

public class Items {

    private static Map<Long, Item> items = new HashMap<>();
    private static Set<Item> destroyedItems = new HashSet<>();
    private static Map<Item, Creature> draggedItems = new HashMap<>();

    public static void reset() {
        items = new HashMap<>();
        destroyedItems = new HashSet<>();
    }

    public static void addItems(Item... allItems) {
        for (Item item : allItems)
            items.put(item.getWurmId(), item);
    }

    public static Item getItem(long wurmId) throws NoSuchItemException {
        if (!items.containsKey(wurmId))
            throw new NoSuchItemException("");
        return items.get(wurmId);
    }

    public static Optional<Item> getItemOptional(long wurmId) {
        if (items.containsKey(wurmId))
            return Optional.of(items.get(wurmId));
        return Optional.empty();
    }

    public static void destroyItem(long wurmId) {
        try {
            Item item = getItem(wurmId);
            destroyedItems.add(item);
            items.remove(wurmId);
            Creature creature = Creatures.getInstance().getCreatureOrNull(item.getOwnerId());
            if (creature != null)
                creature.getInventory().getItems().remove(item);
        } catch (NoSuchItemException e) {
            System.out.println("Item not found.");
            throw new RuntimeException(e);
        }
    }

    public static boolean wasDestroyed(Item item) {
        return destroyedItems.contains(item);
    }

    public static void startDragging(Creature creature, Item item) {
        draggedItems.put(item, creature);
        creature.setDraggedItem(item);
    }

    public static void stopDragging(Item dragged) {
        Creature creature = draggedItems.get(dragged);
        if (creature != null) {
            draggedItems.remove(dragged);
            creature.setDraggedItem(null);
        }
    }
}
