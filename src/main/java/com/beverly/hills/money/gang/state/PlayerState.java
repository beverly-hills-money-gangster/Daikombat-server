package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class PlayerState {

    private static final int DEFAULT_HP = 100;

    private final AtomicInteger kills = new AtomicInteger();
    private final AtomicInteger health = new AtomicInteger(DEFAULT_HP);

    @Getter
    private final int id;

    @Getter
    private final String name;
    private final AtomicReference<PlayerCoordinates> playerCoordinatesRef;

    public PlayerState(String name, PlayerCoordinates coordinates, int id) {
        this.name = name;
        this.playerCoordinatesRef = new AtomicReference<>(coordinates);
        this.id = id;
    }

    public void setPlayerCoordinates(PlayerCoordinates playerCoordinates) {
        playerCoordinatesRef.set(playerCoordinates);
    }

    public void registerKill() {
        kills.incrementAndGet();
    }

    @Builder
    public static class PlayerCoordinates {
        private final Vector2D direction;
        private final Vector2D position;
    }

}
