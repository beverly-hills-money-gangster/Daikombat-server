package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class JoinGameTest extends AbstractGameServerTest {
    @Test
    public void testJoinGame() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(50);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(),
                "Should be exactly 1 response: my spawn");

        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        assertEquals(1, mySpawn.getGameEvents().getEventsCount(), "Should be only my spawn");
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        assertEquals("my player name", mySpawnGameEvent.getPlayer().getPlayerName());
        assertEquals(100, mySpawnGameEvent.getPlayer().getHealth());


        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(50);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(GameConfig.GAMES_TO_CREATE, games.size());
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "It's only me now");
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            if (gameInfo.getGameId() != gameIdToConnectTo) {
                assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
            }
        }
    }

    @Test
    public void testJoinGameNotExistingGame() throws IOException, InterruptedException {
        int gameIdToConnectTo = 666;
        GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(50);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.NOT_EXISTING_GAME_ROOM.ordinal(), errorEvent.getErrorCode(),
                "Should a non-existing game error");
        assertEquals("Not existing game room", errorEvent.getMessage());

        // need a new game connection because the previous is closed
        var newGameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(50);
        assertEquals(0, newGameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, newGameConnection.getResponse().size(), "Should be exactly one response");

        ServerResponse gamesInfoServerResponse = newGameConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = gamesInfoServerResponse.getServerInfo().getGamesList();
        assertEquals(GameConfig.GAMES_TO_CREATE, games.size());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
        }
    }

    @Test
    public void testJoinGameTooMany() throws IOException, InterruptedException {
        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME; i++) {
            GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
            gameConnection.write(
                    JoinGameCommand.newBuilder()
                            .setPlayerName("my player name " + i)
                            .setGameId(0).build());
        }

        GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(0).build());
        Thread.sleep(50);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.SERVER_FULL.ordinal(), errorEvent.getErrorCode(),
                "Should a server full error");
        assertEquals("Can't connect player. Server is full.", errorEvent.getMessage());
    }

    @Test
    public void testJoinGameMultiplePlayers() throws Exception {
        int gameIdToConnectTo = 0;
        Map<Integer, ServerResponse.Vector> connectedPlayersPositions = new HashMap<>();
        for (int i = 0; i < GameConfig.MAX_PLAYERS_PER_GAME - 1; i++) {
            GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
            gameConnection.write(
                    JoinGameCommand.newBuilder()
                            .setPlayerName("my player name " + i)
                            .setGameId(gameIdToConnectTo).build());
            Thread.sleep(150);
            ServerResponse mySpawnResponse = gameConnection.getResponse().poll().get();
            var mySpawnEvent = mySpawnResponse.getGameEvents().getEvents(0);
            connectedPlayersPositions.put(mySpawnEvent.getPlayer().getPlayerId(), mySpawnEvent.getPlayer().getPosition());
        }
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME - 1, connectedPlayersPositions.size(),
                "All players must have unique ids. Something is off");

        GameConnection gameConnection = createGameConnection(GameConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(50);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(2, gameConnection.getResponse().size(), "Should be 2 responses: my spawn + all other players stats");
        ServerResponse mySpawnResponse = gameConnection.getResponse().poll().get();
        assertEquals(1, mySpawnResponse.getGameEvents().getEventsCount(), "Should be my spawn event only");
        var mySpawnEvent = mySpawnResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, mySpawnEvent.getEventType(),
                "Should be my spawn");
        assertFalse(mySpawnEvent.hasAffectedPlayer(), "My spawn does not affect any player");
        assertEquals(100, mySpawnEvent.getPlayer().getHealth());
        assertTrue(mySpawnEvent.getPlayer().hasDirection(), "Should be a direction vector specified");
        assertTrue(mySpawnEvent.getPlayer().hasPosition(), "Should be a position vector specified");


        ServerResponse allOtherPlayersResponse = gameConnection.getResponse().poll().get();
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, allOtherPlayersResponse.getGameEvents().getPlayersOnline());
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME - 1, allOtherPlayersResponse.getGameEvents().getEventsCount(),
                "Should be spawns of all players but me");
        Set<Integer> spawnedPlayersIds = new HashSet<>();
        allOtherPlayersResponse.getGameEvents().getEventsList().forEach(gameEvent -> {
            assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, gameEvent.getEventType());
            assertEquals(100, gameEvent.getPlayer().getHealth(),
                    "Nobody got shot so everybody has health 100%");
            assertEquals(connectedPlayersPositions.get(gameEvent.getPlayer().getPlayerId()),
                    gameEvent.getPlayer().getPosition(),
                    "Nobody moved so the position must be the same as in the beginning");
            spawnedPlayersIds.add(gameEvent.getPlayer().getPlayerId());
        });

        assertEquals(connectedPlayersPositions.keySet(), spawnedPlayersIds,
                "All spawned players should be returned in the response");

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(50);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(GameConfig.GAMES_TO_CREATE, games.size());
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, myGame.getPlayersOnline(), "We should connect all players");

        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            if (gameInfo.getGameId() != gameIdToConnectTo) {
                assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
            }
        }
    }
}
