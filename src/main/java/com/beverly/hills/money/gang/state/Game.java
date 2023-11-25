package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.spawner.Spawner;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class Game {

    private static final int MAX_PLAYERS_TO_ADD = 25;
    private final AtomicInteger playerIdGenerator = new AtomicInteger();
    private final AtomicLong gameStateId = new AtomicLong();

    private final Spawner spawner;

    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();

    public PlayerConnectedGameState connectPlayer(final String playerName) throws GameLogicError {
        if (players.size() >= MAX_PLAYERS_TO_ADD) {
            throw new GameLogicError("Can't connect player. Server is full.", GameErrorCode.SERVER_FULL);
        }
        int playerId = playerIdGenerator.incrementAndGet();
        PlayerState.PlayerCoordinates spawn = spawner.spawn();
        if (players.putIfAbsent(playerName, new PlayerState(playerName, spawn, playerId)) == null) {
            return PlayerConnectedGameState.builder()
                    .newGameStateId(getNewSequenceId())
                    .connectedPlayerId(playerId)
                    .playerName(playerName)
                    .spawn(spawn).build();
        } else {
            throw new GameLogicError("Can't connect player. Try another player name.", GameErrorCode.PLAYER_EXISTS);
        }
    }

    // TODO return an object instead
    public PlayerShootingGameState shoot(final PlayerState.PlayerCoordinates shootingPlayerCoordinates,
                                         final int shootingPlayerId,
                                         final Integer shotPlayerId) {
        move(shootingPlayerId, shootingPlayerCoordinates);

        if (shotPlayerId == null) {
            return PlayerShootingGameState.builder()
                    .newGameStateId(getNewSequenceId())
                    .shootingPlayerId(shootingPlayerId)
                    .playerShot(null).build();
        }
        var details = getPlayer(shotPlayerId).map(shotPlayer -> {
            var shotDetails = shotPlayer.getShot();
            if (shotDetails.isDead()) {
                getPlayer(shootingPlayerId).ifPresent(PlayerState::registerKill);
            }
            return shotDetails;
        }).orElse(null);

        if (details == null) {
            return PlayerShootingGameState.builder()
                    .newGameStateId(getNewSequenceId())
                    .shootingPlayerId(shootingPlayerId)
                    .playerShot(null).build();
        }
        return PlayerShootingGameState.builder()
                .newGameStateId(getNewSequenceId())
                .shootingPlayerId(shootingPlayerId)
                .playerShot(PlayerShootingGameState.PlayerShot.builder()
                        .dead(details.isDead()).shotPlayerId(shotPlayerId)
                        .health(details.getHealth())
                        .build()).build();
    }

    // TODO return an object instead
    private void move(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
        getPlayer(movingPlayerId).ifPresent(playerState -> playerState.move(playerCoordinates));
    }

    public int playersOnline() {
        return players.size();
    }

    public long getNewSequenceId() {
        return gameStateId.incrementAndGet();
    }

    public Stream<PlayerStateReader> readPlayers() {
        return players.values().stream().map(playerState -> playerState);
    }

    private Optional<PlayerState> getPlayer(int playerId) {
        return players.values().stream()
                .filter(playerState -> playerState.getPlayerId() == playerId)
                .findFirst();
    }

    public Optional<PlayerStateReader> readPlayer(int playerId) {
        return getPlayer(playerId).map(playerState -> playerState);
    }


}
