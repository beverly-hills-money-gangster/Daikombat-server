package com.beverly.hills.money.gang.factory;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.state.GameState;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.Vector;

import java.util.List;
import java.util.stream.Stream;

public interface ServerEventsFactory {

    static ServerEvents.GameEvent.Vector createVector(Vector vector) {
        return ServerEvents.GameEvent.Vector.newBuilder()
                .setX(vector.getX())
                .setY(vector.getY()).build();
    }

    static ServerEvents.GameEvent createSpawnEvent(int playerId,
                                                   String playerName,
                                                   PlayerState.PlayerCoordinates coordinates) {
        return ServerEvents.GameEvent.newBuilder()
                .setEventType(ServerEvents.GameEvent.GameEventType.SPAWN)
                .setPlayerId(playerId)
                .setPlayerName(playerName)
                .setPosition(ServerEventsFactory.createVector(coordinates.getPosition()))
                .setDirection(ServerEventsFactory.createVector(coordinates.getDirection()))
                .build();
    }

    static ServerEvents createErrorEvent(GameLogicError error) {
        return ServerEvents.newBuilder()
                .setError(ServerEvents.Error.newBuilder()
                        .setErrorCode(error.getErrorCode().ordinal())
                        .setMessage(error.getMessage())
                        .build())
                .build();
    }

    static ServerEvents createSpawnEventAllPlayers(long eventId,
                                                   int playersOnline,
                                                   Stream<PlayerStateReader> playersSate) {
        var allPlayersSpawns = ServerEvents.GameEvents.newBuilder();
        playersSate.forEach(playerStateReader -> allPlayersSpawns.addEvents(createSpawnEvent(
                playerStateReader.getPlayerId(),
                playerStateReader.getPlayerName(),
                playerStateReader.getCoordinates())));

        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(allPlayersSpawns)
                .build();
    }

    static ServerEvents createSpawnEventSinglePlayer(int playersOnline,
                                                     GameState.PlayerConnectedGameState playerConnected) {
        return ServerEvents.newBuilder()
                .setEventId(playerConnected.getNewGameStateId())
                .setPlayersOnline(playersOnline)
                .setGameEvents(ServerEvents.GameEvents.newBuilder()
                        .addEvents(createSpawnEvent(
                                playerConnected.getConnectedPlayerId(),
                                playerConnected.getPlayerName(),
                                playerConnected.getSpawn())))
                .build();
    }
}
