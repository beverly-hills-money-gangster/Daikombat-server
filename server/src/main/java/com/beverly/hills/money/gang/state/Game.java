package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.registry.PlayersRegistry;
import com.beverly.hills.money.gang.spawner.Spawner;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Game implements Closeable, GameReader {

    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    @Getter
    private final int id;
    private final AtomicInteger playerIdGenerator = new AtomicInteger();

    @Getter
    private final PlayersRegistry playersRegistry = new PlayersRegistry();

    private final Spawner spawner = new Spawner();

    private final AtomicBoolean gameClosed = new AtomicBoolean();

    public PlayerConnectedGameState connectPlayer(final String playerName, final Channel playerChannel) throws GameLogicError {
        validateGameNotClosed();
        int playerId = playerIdGenerator.incrementAndGet();
        PlayerState.PlayerCoordinates spawn = spawner.spawn();
        PlayerState connectedPlayerState = new PlayerState(playerName, spawn, playerId);
        playersRegistry.addPlayer(connectedPlayerState, playerChannel);
        return PlayerConnectedGameState.builder()
                .playerStateReader(connectedPlayerState).build();
    }

    public PlayerShootingGameState shoot(final PlayerState.PlayerCoordinates shootingPlayerCoordinates,
                                         final int shootingPlayerId,
                                         final Integer shotPlayerId) throws GameLogicError {
        validateGameNotClosed();
        PlayerState shootingPlayerState = getPlayer(shootingPlayerId).orElse(null);
        if (shootingPlayerState == null) {
            LOG.warn("Non-existing player can't shoot");
            return null;
        } else if (shootingPlayerState.isDead()) {
            LOG.warn("Dead players can't shoot");
            return null;
        } else if (Objects.equals(shootingPlayerId, shotPlayerId)) {
            LOG.warn("You can't shoot yourself");
            throw new GameLogicError("You can't shoot yourself", GameErrorCode.CAN_NOT_SHOOT_YOURSELF);
        }

        move(shootingPlayerId, shootingPlayerCoordinates);
        if (shotPlayerId == null) {
            LOG.debug("Nobody got shot");
            // if nobody was shot
            return PlayerShootingGameState.builder()
                    .shootingPlayer(shootingPlayerState)
                    .playerShot(null).build();
        }
        var shotPlayerState = getPlayer(shotPlayerId).map(shotPlayer -> {
            if (shotPlayer.isDead()) {
                LOG.warn("You can't shoot a dead player");
                return null;
            }
            shotPlayer.getShot();
            if (shotPlayer.isDead()) {
                shootingPlayerState.registerKill();
            }
            return shotPlayer;
        }).orElse(null);

        if (shotPlayerState == null) {
            LOG.warn("Can't shoot a non-existing player");
            return null;
        }
        return PlayerShootingGameState.builder()
                .shootingPlayer(shootingPlayerState)
                .playerShot(shotPlayerState)
                .build();
    }

    public void bufferMove(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) throws GameLogicError {
        validateGameNotClosed();
        move(movingPlayerId, playerCoordinates);
    }

    public List<PlayerStateReader> getBufferedMoves() {
        return playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .filter(PlayerState::hasMoved).collect(Collectors.toList());
    }

    public void flushBufferedMoves() {
        playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .forEach(PlayerState::flushMove);
    }

    @Override
    public int gameId() {
        return id;
    }

    public int playersOnline() {
        return playersRegistry.playersOnline();
    }

    @Override
    public int maxPlayersAvailable() {
        return ServerConfig.MAX_PLAYERS_PER_GAME;
    }

    public Stream<PlayerStateReader> readPlayers() {
        return playersRegistry.allPlayers().map(PlayersRegistry.PlayerStateChannel::getPlayerState)
                .map(playerState -> playerState);
    }


    public Optional<PlayerStateReader> readPlayer(int playerId) {
        return getPlayer(playerId).map(playerState -> playerState);
    }


    @Override
    public void close() {
        if (!gameClosed.compareAndSet(false, true)) {
            LOG.warn("Game already closed");
            return;
        }
        LOG.info("Close game {}", getId());
        playersRegistry.close();
        gameClosed.set(true);
    }


    private void validateGameNotClosed() throws GameLogicError {
        if (gameClosed.get()) {
            throw new GameLogicError("Game is closed", GameErrorCode.GAME_CLOSED);
        }
    }

    private Optional<PlayerState> getPlayer(int playerId) {
        return playersRegistry.getPlayerState(playerId);
    }

    private void move(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        getPlayer(movingPlayerId)
                .filter(playerState -> !playerState.isDead())
                .ifPresent(playerState -> playerState.move(playerCoordinates));
    }
}
