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
public class GameState {

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
            return new PlayerConnectedGameState(getNewSequenceId(), playerName, playerId, spawn);
        } else {
            throw new GameLogicError("Can't connect player. Try another player name.", GameErrorCode.PLAYER_EXISTS);
        }
    }

    // TODO return an object instead
    public boolean shoot(final int shootingPlayerId, final int shotPlayerId) {
        return getPlayer(shotPlayerId).map(shotPlayer -> {
            if (shotPlayer.getShot()) {
                getPlayer(shootingPlayerId).ifPresent(PlayerState::registerKill);
                return true;
            } else {
                return false;
            }
        }).orElse(false);
    }

    // TODO return an object instead
    public void move(final int movingPlayerId, final PlayerState.PlayerCoordinates playerCoordinates) {
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


    public static class PlayerConnectedGameState extends GameStateId {
        private final int connectedPlayerId;
        private final String playerName;
        private final PlayerState.PlayerCoordinates spawn;

        public PlayerConnectedGameState(long newGameStateId,
                                        String playerName,
                                        int connectedPlayerId,
                                        PlayerState.PlayerCoordinates spawn) {
            super(newGameStateId);
            this.connectedPlayerId = connectedPlayerId;
            this.playerName = playerName;
            this.spawn = spawn;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getConnectedPlayerId() {
            return connectedPlayerId;
        }

        public PlayerState.PlayerCoordinates getSpawn() {
            return spawn;
        }
    }

    public static abstract class GameStateId {
        private final long newGameStateId;

        public GameStateId(long newGameStateId) {
            this.newGameStateId = newGameStateId;
        }

        public long getNewGameStateId() {
            return newGameStateId;
        }
    }


}
