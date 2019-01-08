package com.wurmonline.server.creatures;

public class Communicator {
    private String lastMessage = "";

    public void sendNormalServerMessage(String message) {
        lastMessage = message;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
