package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.PlayerState;
import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.beverly.hills.money.gang.config.ServerConfig.MAX_PLAYERS_PER_GAME;

public class PlayersRegistry implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(PlayersRegistry.class);

    private final Map<Integer, PlayerStateChannel> players = new ConcurrentHashMap<>();

    public void addPlayer(PlayerState playerState, Channel channel) throws GameLogicError {
        LOG.info("Add player {}", playerState);
        // not thread-safe
        if (players.size() >= MAX_PLAYERS_PER_GAME) {
            throw new GameLogicError("Can't connect player. Server is full.", GameErrorCode.SERVER_FULL);
        } else if (players.values().stream()
                .anyMatch(playerStateChannel -> playerStateChannel.getPlayerState().getPlayerName()
                        .equals(playerState.getPlayerName()))) {
            throw new GameLogicError("Can't connect player. Player name already taken.", GameErrorCode.PLAYER_EXISTS);
        }
        // thread-safe
        players.put(playerState.getPlayerId(), PlayerStateChannel.builder()
                .channel(channel).playerState(playerState).build());
    }

    public Optional<PlayerState> getPlayerState(int playerId) {
        return Optional.ofNullable(players.get(playerId)).map(playerStateChannel -> playerStateChannel.playerState);
    }

    public Stream<PlayerStateChannel> allPlayers() {
        return players.values().stream();
    }

    public int playersOnline() {
        return players.size();
    }

    public boolean playerExists(Channel channel, int playerId) {
        return Optional.ofNullable(players.get(playerId))
                .map(playerStateChannel -> playerStateChannel.channel == channel)
                .orElse(false);
    }

    public Optional<PlayerState> removePlayer(int playerId) {
        LOG.info("Remove player {}", playerId);
        PlayerStateChannel playerStateChannel = players.remove(playerId);
        if (playerStateChannel != null) {
            playerStateChannel.getChannel().close();
            return Optional.of(playerStateChannel.playerState);
        }
        return Optional.empty();
    }

    @Override
    public void close() {
        LOG.info("Close registry");
        players.values().forEach(playerStateChannel -> playerStateChannel.getChannel().close());
        players.clear();
    }

    @Builder
    @Getter
    @ToString
    public static class PlayerStateChannel {
        private final Channel channel;
        private final PlayerState playerState;
    }
}
