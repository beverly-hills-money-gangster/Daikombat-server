package com.beverly.hills.money.gang.factory;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.PlayerConnectedGameState;
import com.beverly.hills.money.gang.state.PlayerStateReader;
import com.beverly.hills.money.gang.state.Vector;

import java.util.List;
import java.util.stream.Stream;

public interface ServerResponseFactory {

    static ServerResponse.Vector createVector(Vector vector) {
        return ServerResponse.Vector.newBuilder()
                .setX(vector.getX())
                .setY(vector.getY()).build();
    }

    static ServerResponse.GameEvent createSpawnEvent(PlayerStateReader playerStateReader) {
        return ServerResponse.GameEvent.newBuilder()
                .setEventType(ServerResponse.GameEvent.GameEventType.SPAWN)
                .setPlayer(createPlayerStats(playerStateReader))
                .build();
    }


    static ServerResponse.GameEvent createMoveGameEvent(PlayerStateReader playerStateReader) {
        return ServerResponse.GameEvent.newBuilder()
                .setPlayer(createPlayerStats(playerStateReader))
                .setEventType(ServerResponse.GameEvent.GameEventType.MOVE).build();
    }

    static ServerResponse.GameEvent createDisconnectGameEvent(PlayerStateReader playerStateReader) {
        return ServerResponse.GameEvent.newBuilder()
                .setPlayer(createPlayerStats(playerStateReader))
                .setEventType(ServerResponse.GameEvent.GameEventType.DISCONNECT).build();
    }

    static ServerResponse createErrorEvent(GameLogicError error) {
        return ServerResponse.newBuilder()
                .setErrorEvent(ServerResponse.ErrorEvent.newBuilder()
                        .setErrorCode(error.getErrorCode().ordinal())
                        .setMessage(error.getMessage())
                        .build())
                .build();
    }

    static ServerResponse createSpawnEventAllPlayers(int playersOnline,
                                                     Stream<PlayerStateReader> playersSate) {
        var allPlayersSpawns = ServerResponse.GameEvents.newBuilder();
        playersSate.forEach(playerStateReader
                -> allPlayersSpawns.addEvents(createSpawnEvent(playerStateReader)));
        allPlayersSpawns.setPlayersOnline(playersOnline);
        return ServerResponse.newBuilder()
                .setGameEvents(allPlayersSpawns)
                .build();
    }

    static ServerResponse createServerInfo(Stream<GameReader> games) {
        var serverInfo = ServerResponse.ServerInfo.newBuilder();
        games.forEach(game
                -> serverInfo.addGames(
                ServerResponse.GameInfo.newBuilder()
                        .setGameId(game.gameId())
                        .setPlayersOnline(game.playersOnline())
                        .setMaxGamePlayers(game.maxPlayersAvailable())
                        .build()));

        return ServerResponse.newBuilder()
                .setServerInfo(serverInfo)
                .build();
    }

    static ServerResponse createMovesEventAllPlayers(int playersOnline,
                                                     Stream<PlayerStateReader> playersSate) {
        var allPlayersMoves = ServerResponse.GameEvents.newBuilder();
        playersSate.forEach(playerStateReader
                -> allPlayersMoves.addEvents(createMoveGameEvent(playerStateReader)));
        allPlayersMoves.setPlayersOnline(playersOnline);
        return ServerResponse.newBuilder()
                .setGameEvents(allPlayersMoves)
                .build();
    }

    static ServerResponse createDisconnectedEvent(int playersOnline,
                                                  Stream<PlayerStateReader> disconnectedPlayers) {
        var allDisconnectedPlayers = ServerResponse.GameEvents.newBuilder();
        disconnectedPlayers.forEach(playerStateReader
                -> allDisconnectedPlayers.addEvents(createDisconnectGameEvent(playerStateReader)));
        allDisconnectedPlayers.setPlayersOnline(playersOnline);
        return ServerResponse.newBuilder()
                .setGameEvents(allDisconnectedPlayers)
                .build();
    }

    static ServerResponse.GameEventPlayerStats createPlayerStats(PlayerStateReader playerReader) {
        return ServerResponse.GameEventPlayerStats.newBuilder()
                .setPlayerName(playerReader.getPlayerName())
                .setPosition(createVector(playerReader.getCoordinates().getPosition()))
                .setDirection(createVector(playerReader.getCoordinates().getDirection()))
                .setHealth(playerReader.getHealth())
                .setPlayerId(playerReader.getPlayerId())
                .build();
    }

    static ServerResponse createDeadEvent(
            PlayerStateReader shooterPlayerReader,
            PlayerStateReader deadPlayerReader) {
        var deadPlayerEvent = ServerResponse.GameEvents.newBuilder()
                .addEvents(ServerResponse.GameEvent.newBuilder()
                        .setEventType(ServerResponse.GameEvent.GameEventType.DEATH)
                        .setPlayer(createPlayerStats(shooterPlayerReader))
                        .setAffectedPlayer(createPlayerStats(deadPlayerReader)));
        return ServerResponse.newBuilder()
                .setGameEvents(deadPlayerEvent)
                .build();
    }

    static ServerResponse createGetShotEvent(int playersOnline,
                                             PlayerStateReader shooterPlayerReader,
                                             PlayerStateReader shotPlayerReader) {
        var deadPlayerEvent = ServerResponse.GameEvents.newBuilder()
                .addEvents(ServerResponse.GameEvent.newBuilder()
                        .setEventType(ServerResponse.GameEvent.GameEventType.GET_SHOT)
                        .setPlayer(createPlayerStats(shooterPlayerReader))
                        .setAffectedPlayer(createPlayerStats(shotPlayerReader)));
        deadPlayerEvent.setPlayersOnline(playersOnline);
        return ServerResponse.newBuilder()
                .setGameEvents(deadPlayerEvent)
                .build();
    }

    static ServerResponse createShootingEvent(int playersOnline,
                                              PlayerStateReader shooterPlayerReader) {
        var deadPlayerEvent = ServerResponse.GameEvents.newBuilder()
                .addEvents(ServerResponse.GameEvent.newBuilder()
                        .setEventType(ServerResponse.GameEvent.GameEventType.SHOOT)
                        .setPlayer(createPlayerStats(shooterPlayerReader)));
        deadPlayerEvent.setPlayersOnline(playersOnline);
        return ServerResponse.newBuilder()
                .setGameEvents(deadPlayerEvent)
                .build();
    }

    static ServerResponse createSpawnEventSinglePlayer(PlayerConnectedGameState playerConnected) {

        return ServerResponse.newBuilder()
                .setGameEvents(ServerResponse.GameEvents.newBuilder()
                        .addEvents(createSpawnEvent(playerConnected.getPlayerStateReader())))
                .build();
    }

    static ServerResponse createChatEvent(String message, int fromPlayerId) {
        return ServerResponse.newBuilder()
                .setChatEvents(ServerResponse.ChatEvent.newBuilder()
                        .setPlayerId(fromPlayerId).setMessage(message)
                        .build())
                .build();
    }
}
