package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.Game;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;

@RequiredArgsConstructor
public class GameRoomRegistry {

    private final Map<Integer, Game> games = new HashMap<>();

    public GameRoomRegistry(int gamesToCreate) {
        for (int i = 0; i < gamesToCreate; i++) {
            games.put(i, new Game());
        }
    }


    public Stream<Game> getGames() {
        return games.values().stream();
    }

    public Game getGame(int gameId) throws GameLogicError {
        return Optional.ofNullable(games.get(gameId))
                .orElseThrow(() -> new GameLogicError("Not existing game room", NOT_EXISTING_GAME_ROOM));
    }
}
