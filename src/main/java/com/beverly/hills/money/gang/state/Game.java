package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.spawner.Spawner;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Game {

    @Getter
    private final int id;
    private static final int MAX_PLAYERS_TO_ADD = 25;
    private final AtomicInteger playerIdGenerator = new AtomicInteger();
    private final AtomicLong gameStateId = new AtomicLong();

    private final Spawner spawner;
    private final Map<Integer, PlayerState> players = new ConcurrentHashMap<>();

    public PlayerConnectedGameState connectPlayer(final String playerName) throws GameLogicError {
        if (players.size() >= MAX_PLAYERS_TO_ADD) {
            throw new GameLogicError("Can't connect player. Server is full.", GameErrorCode.SERVER_FULL);
        }
        int playerId = playerIdGenerator.incrementAndGet();
        PlayerState.PlayerCoordinates spawn = spawner.spawn();
        PlayerState connectedPlayerState = new PlayerState(playerName, spawn, playerId);
        players.put(playerId, connectedPlayerState);
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
        // TODO remove player if it's dead
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
                players.remove(shootingPlayerId);
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

    private void move(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        getPlayer(movingPlayerId).ifPresent(playerState -> playerState.move(playerCoordinates));
    }

    public void bufferMove(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        move(movingPlayerId, playerCoordinates);
        newSequenceId();
    }

    public Stream<PlayerStateReader> getBufferedMoves() {
        return players.values().stream().filter(PlayerState::hasMoved).map(playerState -> playerState);
    }

    public void flushBufferedMoves() {
        players.values().forEach(PlayerState::flushMove);
    }

    public int playersOnline() {
        return players.size();
    }

    public long newSequenceId() {
        return gameStateId.incrementAndGet();
    }

    public Stream<PlayerStateReader> readPlayers() {
        return players.values().stream().map(playerState -> playerState);
    }

    private Optional<PlayerState> getPlayer(int playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public Optional<PlayerStateReader> readPlayer(int playerId) {
        return getPlayer(playerId).map(playerState -> playerState);
    }


}
