package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.beverly.hills.money.gang.config.ServerConfig.MAX_IDLE_TIME_MLS;

@ToString
public class PlayerState implements PlayerStateReader {

    private final AtomicLong stateChangedLastTime = new AtomicLong(System.currentTimeMillis());

    private final AtomicBoolean moved = new AtomicBoolean(false);

    private final AtomicBoolean dead = new AtomicBoolean();
    public static final int DEFAULT_HP = 100;

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

    public int getKills() {
        return kills.get();
    }

    public boolean isIdleForTooLong() {
        return (System.currentTimeMillis() - stateChangedLastTime.get()) > MAX_IDLE_TIME_MLS;
    }

    public void getShot() {
        stateChangedLastTime.set(System.currentTimeMillis());
        if (health.addAndGet(-ServerConfig.DEFAULT_DAMAGE) <= 0) {
            dead.set(true);
        }
    }


    public void move(PlayerCoordinates playerCoordinates) {
        stateChangedLastTime.set(System.currentTimeMillis());
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
        stateChangedLastTime.set(System.currentTimeMillis());
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
