package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.spawner.Spawner;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Game implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    @Getter
    private final int id;
    private final AtomicInteger playerIdGenerator = new AtomicInteger();
    private final AtomicLong gameStateId = new AtomicLong();

    @Getter
    private final PlayersRegistry playersRegistry = new PlayersRegistry();

    private final Spawner spawner = new Spawner();

    public PlayerConnectedGameState connectPlayer(final String playerName, final Channel playerChannel) throws GameLogicError {
        int playerId = playerIdGenerator.incrementAndGet();
        PlayerState.PlayerCoordinates spawn = spawner.spawn();
        PlayerState connectedPlayerState = new PlayerState(playerName, spawn, playerId);
        playersRegistry.addPlayer(connectedPlayerState, playerChannel);
        return PlayerConnectedGameState.builder()
                .playerStateReader(connectedPlayerState)
                .newGameStateId(newSequenceId()).build();
    }

    public PlayerShootingGameState shoot(final PlayerState.PlayerCoordinates shootingPlayerCoordinates,
                                         final int shootingPlayerId,
                                         final Integer shotPlayerId) {
        PlayerState shootingPlayerState = getPlayer(shootingPlayerId).orElse(null);
        if (shootingPlayerState == null) {
            return null;
        }
        move(shootingPlayerId, shootingPlayerCoordinates);
        if (shotPlayerId == null) {
            return PlayerShootingGameState.builder()
                    .newGameStateId(newSequenceId())
                    .shootingPlayer(shootingPlayerState)
                    .playerShot(null).build();
        }
        var shotPlayerState = getPlayer(shotPlayerId).map(shotPlayer -> {
            var state = shotPlayer.getShot();
            if (state.isDead()) {
                shootingPlayerState.registerKill();
            }
            return state;
        }).orElse(null);

        if (shotPlayerState == null) {
            return PlayerShootingGameState.builder()
                    .newGameStateId(newSequenceId())
                    .shootingPlayer(shootingPlayerState)
                    .playerShot(null).build();
        }
        return PlayerShootingGameState.builder()
                .newGameStateId(newSequenceId())
                .shootingPlayer(shootingPlayerState)
                .playerShot(shotPlayerState)
                .build();
    }

    public void bufferMove(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        move(movingPlayerId, playerCoordinates);
        newSequenceId();
    }

    public Stream<PlayerStateReader> getBufferedMoves() {
        return playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .filter(PlayerState::hasMoved).map(playerState -> playerState);
    }

    public void flushBufferedMoves() {
        playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .forEach(PlayerState::flushMove);
    }

    public int playersOnline() {
        return playersRegistry.playersOnline();
    }

    public long newSequenceId() {
        return gameStateId.incrementAndGet();
    }

    private void move(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        getPlayer(movingPlayerId).ifPresent(playerState -> playerState.move(playerCoordinates));
    }

    public Stream<PlayerStateReader> readPlayers() {
        return playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .map(playerState -> playerState);
    }

    private Optional<PlayerState> getPlayer(int playerId) {
        return playersRegistry.getPlayerState(playerId);
    }

    public Optional<PlayerStateReader> readPlayer(int playerId) {
        return getPlayer(playerId).map(playerState -> playerState);
    }


    @Override
    public void close() {
        LOG.info("Close game {}", getId());
        playersRegistry.close();
    }
}
