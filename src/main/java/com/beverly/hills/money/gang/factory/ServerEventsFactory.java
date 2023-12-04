package com.beverly.hills.money.gang.factory;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerEvents;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.Vector;

import java.util.stream.Stream;

public interface ServerEventsFactory {

    static ServerEvents.Vector createVector(Vector vector) {
        return ServerEvents.Vector.newBuilder()
                .setX(vector.getX())
                .setY(vector.getY()).build();
    }

    static ServerEvents.GameEvent createSpawnEvent(PlayerStateReader playerStateReader) {
        return ServerEvents.GameEvent.newBuilder()
                .setEventType(ServerEvents.GameEvent.GameEventType.SPAWN)
                .setPlayer(createPlayerStats(playerStateReader))
                .build();
    }


    static ServerEvents.GameEvent createMoveGameEvent(PlayerStateReader playerStateReader) {
        return ServerEvents.GameEvent.newBuilder()
                .setPlayer(createPlayerStats(playerStateReader))
                .setEventType(ServerEvents.GameEvent.GameEventType.MOVE).build();
    }

    static ServerEvents.GameEvent createDisconnectGameEvent(PlayerStateReader playerStateReader) {
        return ServerEvents.GameEvent.newBuilder()
                .setPlayer(createPlayerStats(playerStateReader))
                .setEventType(ServerEvents.GameEvent.GameEventType.DISCONNECT).build();
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
        playersSate.forEach(playerStateReader
                -> allPlayersSpawns.addEvents(createSpawnEvent(playerStateReader)));

        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(allPlayersSpawns)
                .build();
    }

    static ServerEvents createMovesEventAllPlayers(long eventId,
                                                   int playersOnline,
                                                   Stream<PlayerStateReader> playersSate) {
        var allPlayersMoves = ServerEvents.GameEvents.newBuilder();
        playersSate.forEach(playerStateReader
                -> allPlayersMoves.addEvents(createMoveGameEvent(playerStateReader)));

        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(allPlayersMoves)
                .build();
    }

    static ServerEvents createDisconnectedEvent(long eventId,
                                                int playersOnline,
                                                Stream<PlayerStateReader> disconnectedPlayers) {
        var allDisconnectedPlayers = ServerEvents.GameEvents.newBuilder();
        disconnectedPlayers.forEach(playerStateReader
                -> allDisconnectedPlayers.addEvents(createDisconnectGameEvent(playerStateReader)));

        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(allDisconnectedPlayers)
                .build();
    }

    static ServerEvents.GameEventPlayerStats createPlayerStats(PlayerStateReader playerReader) {
        return ServerEvents.GameEventPlayerStats.newBuilder()
                .setPlayerName(playerReader.getPlayerName())
                .setPosition(createVector(playerReader.getCoordinates().getPosition()))
                .setDirection(createVector(playerReader.getCoordinates().getDirection()))
                .setHealth(playerReader.getHealth())
                .setPlayerId(playerReader.getPlayerId())
                .build();
    }

    static ServerEvents createDeadEvent(long eventId,
                                        int playersOnline,
                                        PlayerStateReader shooterPlayerReader,
                                        PlayerStateReader deadPlayerReader) {
        var deadPlayerEvent = ServerEvents.GameEvents.newBuilder()
                .addEvents(ServerEvents.GameEvent.newBuilder()
                        .setEventType(ServerEvents.GameEvent.GameEventType.DEATH)
                        .setPlayer(createPlayerStats(shooterPlayerReader))
                        .setAffectedPlayer(createPlayerStats(deadPlayerReader)));
        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(deadPlayerEvent)
                .build();
    }

    static ServerEvents createGetShotEvent(long eventId,
                                           int playersOnline,
                                           PlayerStateReader shooterPlayerReader,
                                           PlayerStateReader shotPlayerReader) {
        var deadPlayerEvent = ServerEvents.GameEvents.newBuilder()
                .addEvents(ServerEvents.GameEvent.newBuilder()
                        .setEventType(ServerEvents.GameEvent.GameEventType.GET_SHOT)
                        .setPlayer(createPlayerStats(shooterPlayerReader))
                        .setAffectedPlayer(createPlayerStats(shotPlayerReader)));
        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(deadPlayerEvent)
                .build();
    }

    static ServerEvents createShootingEvent(long eventId,
                                            int playersOnline,
                                            PlayerStateReader shooterPlayerReader) {
        var deadPlayerEvent = ServerEvents.GameEvents.newBuilder()
                .addEvents(ServerEvents.GameEvent.newBuilder()
                        .setEventType(ServerEvents.GameEvent.GameEventType.SHOOT)
                        .setPlayer(createPlayerStats(shooterPlayerReader)));
        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setGameEvents(deadPlayerEvent)
                .build();
    }

    static ServerEvents createSpawnEventSinglePlayer(int playersOnline,
                                                     PlayerConnectedGameState playerConnected) {

        return ServerEvents.newBuilder()
                .setEventId(playerConnected.getNewGameStateId())
                .setPlayersOnline(playersOnline)
                .setGameEvents(ServerEvents.GameEvents.newBuilder()
                        .addEvents(createSpawnEvent(playerConnected.getPlayerStateReader())))
                .build();
    }

    static ServerEvents createChatEvent(long eventId, int playersOnline, String message, String fromPlayerName) {
        return ServerEvents.newBuilder()
                .setEventId(eventId)
                .setPlayersOnline(playersOnline)
                .setChatEvents(ServerEvents.ChatEvent.newBuilder()
                        .setPlayerName(fromPlayerName).setMessage(message) // TODO maybe use player id instead of name?
                        .build())
                .build();
    }
}
