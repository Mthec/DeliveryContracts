package com.wurmonline.server.items;

import javax.annotation.Nullable;

public class ItemFactory {

    public static Item createItem(int templateId, float ql, @Nullable String creator) {
        return new Item(templateId);
    }

    public static Item createItem(int templateId, float ql, byte material, byte rarity, @Nullable String creator) {
        return new Item(templateId);
    }
}
