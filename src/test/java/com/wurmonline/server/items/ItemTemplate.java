package com.wurmonline.server.items;

public class ItemTemplate {

    int templateId;
    String name;

    public ItemTemplate(int templateId, String name) {
        this.templateId = templateId;
        this.name = name;
    }

    public boolean isContainerWithSubItems() {
        return false;
    }

    public byte getMaterial() {
        return 0;
    }

    public int getTemplateId() {
        return templateId;
    }

    public String getName() {
        return name;
    }
}
