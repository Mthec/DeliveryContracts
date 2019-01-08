package org.gotti.wurmunlimited.modsupport.actions;

import com.wurmonline.server.behaviours.ActionEntry;

public class ModActions {

    private static int nextActionId = 12;

    public static void registerAction(ActionEntry e) {}

    public static void registerAction(ModAction e) {}

    public static int getNextActionId() {
        return nextActionId++;
    }
}
