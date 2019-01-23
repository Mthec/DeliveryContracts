package com.wurmonline.server.items;

public class ItemTemplate {

    int templateId;

    public ItemTemplate(int templateId) {
        this.templateId = templateId;
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
}
