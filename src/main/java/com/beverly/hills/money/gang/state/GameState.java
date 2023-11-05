package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.spawner.Spawner;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class GameState {
    private final AtomicInteger playerIdGenerator = new AtomicInteger();

    private final Spawner spawner;

    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();

    public int connectPlayer(final String playerName) {
        int playerId = playerIdGenerator.incrementAndGet();
        if (players.putIfAbsent(playerName, new PlayerState(playerName, spawner.spawn(), playerId)) == null) {
            return playerId;
        } else {
            throw new
        }
    }
}
