package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.spawner.Spawner;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class GameState {

    private static final int MAX_PLAYERS_TO_ADD = 25;
    private final AtomicInteger playerIdGenerator = new AtomicInteger();
    private final AtomicLong gameStateId = new AtomicLong();

    // TODO don't forget to initialize it
    private final Spawner spawner = null;

    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();

    public PlayerConnectedGameState connectPlayer(final String playerName) throws GameLogicError {
        if (players.size() >= MAX_PLAYERS_TO_ADD) {
            throw new GameLogicError("Can't connect player. Server is full.", GameErrorCode.SERVER_FULL);
        }
        int playerId = playerIdGenerator.incrementAndGet();
        PlayerState.PlayerCoordinates spawn = spawner.spawn();
        if (players.putIfAbsent(playerName, new PlayerState(playerName, spawn, playerId)) == null) {
            return new PlayerConnectedGameState(
                    gameStateId.incrementAndGet(), playerName, playerId, spawn);
        } else {
            throw new GameLogicError("Can't connect player. Try another player name.", GameErrorCode.PLAYER_EXISTS);
        }
    }

    public int playersOnline() {
        return players.size();
    }

    public Stream<PlayerStateReader> readPlayers() {
        return players.values().stream().map(playerState -> playerState);
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
