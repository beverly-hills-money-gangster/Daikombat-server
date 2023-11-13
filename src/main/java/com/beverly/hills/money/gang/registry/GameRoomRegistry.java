package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.state.GameState;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class GameRoomRegistry {

    private final Map<Integer, GameState> games = new HashMap<>();

    public GameRoomRegistry(int gamesToCreate) {
        for (int i = 0; i < gamesToCreate; i++) {
            games.put(i, new GameState());
        }
    }

    public Optional<GameState> getGame(int gameId) {
        return Optional.ofNullable(games.get(gameId));
    }
}
