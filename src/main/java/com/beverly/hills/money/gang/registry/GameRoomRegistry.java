package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.GameState;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;

@RequiredArgsConstructor
public class GameRoomRegistry {

    private final Map<Integer, GameState> games = new HashMap<>();

    public GameRoomRegistry(int gamesToCreate) {
        for (int i = 0; i < gamesToCreate; i++) {
            games.put(i, new GameState());
        }
    }

    public Set<Integer> getGameIds() {
        return games.keySet();
    }

    public GameState getGame(int gameId) throws GameLogicError {
        return Optional.ofNullable(games.get(gameId))
                .orElseThrow(() -> new GameLogicError("Not existing game room", NOT_EXISTING_GAME_ROOM));
    }
}
