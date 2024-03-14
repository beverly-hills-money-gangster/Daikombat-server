package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@ToString
public class PlayerState implements PlayerStateReader {

    private static final Logger LOG = LoggerFactory.getLogger(PlayerState.class);

    public static final int VAMPIRE_HP_BOOST = 20;

    private final AtomicBoolean moved = new AtomicBoolean(false);

    private final AtomicBoolean dead = new AtomicBoolean();
    public static final int DEFAULT_HP = 100;

    private final AtomicInteger kills = new AtomicInteger();
    private final AtomicInteger deaths = new AtomicInteger();
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

    public void respawn(final PlayerCoordinates coordinates) {
        this.playerCoordinatesRef.set(coordinates);
        health.set(DEFAULT_HP);
        dead.set(false);
    }

    public int getKills() {
        return kills.get();
    }

    public int getDeaths() {
        return deaths.get();
    }

    public void getShot() {
        if (health.addAndGet(-ServerConfig.DEFAULT_SHOTGUN_DAMAGE) <= 0) {
            deaths.incrementAndGet();
            dead.set(true);
        }
    }

    public void getPunched() {
        if (health.addAndGet(-ServerConfig.DEFAULT_PUNCH_DAMAGE) <= 0) {
            deaths.incrementAndGet();
            dead.set(true);
        }
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
        vampireBoost();
    }

    private void vampireBoost() {
        int currentHealth = health.get();
        boolean set = health.compareAndSet(currentHealth, Math.min(DEFAULT_HP, currentHealth + VAMPIRE_HP_BOOST));
        if (!set) {
            vampireBoost();
        }
    }

    @Builder
    @ToString
    @EqualsAndHashCode
    public static class PlayerCoordinates {
        @Getter
        private final Vector direction;
        @Getter
        private final Vector position;

    }
}
