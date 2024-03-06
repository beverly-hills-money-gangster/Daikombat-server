package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.beverly.hills.money.gang.exception.GameErrorCode.NOT_EXISTING_GAME_ROOM;

@Component
public class GameRoomRegistry implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(GameRoomRegistry.class);
    private final Map<Integer, Game> games = new HashMap<>();

    private final ApplicationContext applicationContext;

    public GameRoomRegistry(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        for (int i = 0; i < ServerConfig.GAMES_TO_CREATE; i++) {
            var game = applicationContext.getBean(Game.class);
            games.put(game.gameId(), game);
        }
    }

    public Stream<Game> getGames() {
        return games.values().stream();
    }

    public Optional<PlayerStateReader> getLiveJoinedPlayer(int gameId, Channel channel, int playerId) {
        return Optional.ofNullable(games.get(gameId))
                .flatMap(game -> game.getPlayersRegistry().findPlayer(channel, playerId))
                .filter(playerStateReader -> !playerStateReader.isDead());
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
                        .removeClosePlayer(playerToRemove.get().getPlayerState().getPlayerId())
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
        LOG.info("Close");
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
