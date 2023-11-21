package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class PlayerState implements PlayerStateReader {

    private final AtomicBoolean dead = new AtomicBoolean();
    private static final int DEFAULT_HP = 100;

    private static final int DEFAULT_DAMAGE = 20;

    private final AtomicInteger kills = new AtomicInteger();
    private final AtomicInteger health = new AtomicInteger(DEFAULT_HP);

    @Getter
    private final int playerId;

    @Getter
    private final String playerName;
    private final AtomicReference<PlayerCoordinates> playerCoordinatesRef;

    public PlayerState(String name, PlayerCoordinates coordinates, int id) {
        this.playerName = name;
        this.playerCoordinatesRef = new AtomicReference<>(coordinates);
        this.playerId = id;
    }

    public boolean getShot() {
        if (health.addAndGet(-DEFAULT_DAMAGE) <= 0) {
            dead.set(true);
            return true;
        }
        return false;
    }


    public void move(PlayerCoordinates playerCoordinates) {
        playerCoordinatesRef.set(playerCoordinates);
    }

    @Override
    public PlayerCoordinates getCoordinates() {
        return playerCoordinatesRef.get();
    }

    @Override
    public int getHealth() {
        return health.get();
    }


    public void registerKill() {
        kills.incrementAndGet();
    }

    @Builder
    public static class PlayerCoordinates {
        @Getter
        private final Vector direction;
        @Getter
        private final Vector position;

    }

}
