package com.wurmonline.server;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;

import java.util.Random;

public class Server {

    private static Server instance;
    public static final Random rand = new Random();

    public static Server getInstance() {
        if (instance == null)
            instance = new Server();

        return instance;
    }

    public Creature getCreature(long wurmId) {
        return Creatures.getInstance().getCreatureOrNull(wurmId);
    }
}
