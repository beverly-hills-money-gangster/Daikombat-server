package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.Game;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;

@RequiredArgsConstructor
public class GameRoomRegistry implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GameRoomRegistry.class);
    private final Map<Integer, Game> games = new HashMap<>();

    public GameRoomRegistry(int gamesToCreate) {
        for (int i = 0; i < gamesToCreate; i++) {
            games.put(i, new Game(i));
        }
    }

    public Stream<Game> getGames() {
        return games.values().stream();
    }

    public Game getGame(int gameId) throws GameLogicError {
        return Optional.ofNullable(games.get(gameId))
                .orElseThrow(() -> new GameLogicError("Not existing game room", NOT_EXISTING_GAME_ROOM));
    }

    @Override
    public void close() {
        games.values().forEach(game -> {
            try {
                game.close();
            } catch (Exception e) {
                LOG.error("Can't close game {}", game.getId(), e);
            }
        });
        games.clear();
    }
}
