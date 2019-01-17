package com.wurmonline.server.creatures;


public class Communicator {
    private String lastMessage = "";

    public void sendNormalServerMessage(String message) {
        System.out.println(message);
        lastMessage = message;
    }

    public void sendSafeServerMessage(String message) {
        System.out.println(message);
        lastMessage = message;
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
