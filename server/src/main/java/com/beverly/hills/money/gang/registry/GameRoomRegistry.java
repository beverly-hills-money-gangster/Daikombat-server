package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerState;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;

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

    public boolean playerJoinedGame(int gameId, Channel channel, int playerId) {
        return Optional.ofNullable(games.get(gameId))
                .map(game -> game.getPlayersRegistry().playerExists(channel, playerId))
                .orElse(false);
    }


    public boolean removeChannel(final Channel channel, final OnPlayerRemoval onFound) {
        boolean playerFound = false;
        for (Game game : games.values()) {
            var playerToRemove = game.getPlayersRegistry()
                    .allPlayers()
                    .filter(playerStateChannel -> playerStateChannel.getChannel() == channel)
                    .findFirst();
            if (playerToRemove.isPresent()) {
                playerFound = true;
                game.getPlayersRegistry()
                        .removePlayer(playerToRemove.get().getPlayerState().getPlayerId())
                        .ifPresent(playerState -> onFound.onRemoval(game, playerState));
                break;
            }
        }
        return playerFound;
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

    @FunctionalInterface
    public interface OnPlayerRemoval {

        void onRemoval(Game game, PlayerState playerState);

    }
}
