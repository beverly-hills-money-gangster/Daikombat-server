package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class PlayerState implements PlayerStateReader {

    private final AtomicBoolean moved = new AtomicBoolean(false);

    private final AtomicBoolean dead = new AtomicBoolean();
    public static final int DEFAULT_HP = 100;

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

    public PlayerStateReader getShot() {
        if (health.addAndGet(-DEFAULT_DAMAGE) <= 0) {
            dead.set(true);
            return this;
        }
        return this;
    }


    public void move(PlayerCoordinates playerCoordinates) {
        playerCoordinatesRef.set(playerCoordinates);
        moved.set(true);
    }

    public void flushMove() {
        moved.set(false);
    }

    @Override
    public PlayerCoordinates getCoordinates() {
        return playerCoordinatesRef.get();
    }

    @Override
    public int getHealth() {
        return health.get();
    }


    @Override
    public boolean isDead() {
        return dead.get();
    }

    @Override
    public boolean hasMoved() {
        return moved.get();
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

    @Builder
    @Getter
    public static class ShotDetails {
        private final int health;
        private final boolean dead;
    }

}
