package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.PlayerState;
import io.netty.channel.Channel;
import lombok.Builder;
import lombok.Getter;

import java.io.Closeable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.beverly.hills.money.gang.config.GameConfig.MAX_PLAYERS_PER_GAME;

// TODO combine this class with  private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();
// TODO remove inactive players
public class PlayersRegistry implements Closeable {

    private final Map<Integer, PlayerStateChannel> players = new ConcurrentHashMap<>();

    public void addPlayer(PlayerState playerState, Channel channel) throws GameLogicError {
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

    public void removePlayer(int playerId) {
        PlayerStateChannel playerStateChannel = players.remove(playerId);
        if (playerStateChannel != null) {
            playerStateChannel.getChannel().close();
        }
    }

    @Override
    public void close() {
        players.values().forEach(playerStateChannel -> playerStateChannel.getChannel().close());
        players.clear();
    }

    @Builder
    @Getter
    public static class PlayerStateChannel {
        private final Channel channel;
        private final PlayerState playerState;
    }
}
