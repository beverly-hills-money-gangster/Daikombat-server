package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SetEnvironmentVariable(key = "GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_PING_FREQUENCY_MLS", value = "99999")
public class JoinGameTest extends AbstractGameServerTest {

    /**
     * @given a running game server
     * @when a player connects to a server
     * @then the player is connected
     */
    @Test
    public void testJoinGame() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(),
                "Should be exactly 1 response: my spawn");

        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        assertEquals(1, mySpawn.getGameEvents().getEventsCount(), "Should be only my spawn");
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        assertEquals("my player name", mySpawnGameEvent.getPlayer().getPlayerName());
        assertEquals(100, mySpawnGameEvent.getPlayer().getHealth());


        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "It's only me now");
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            if (gameInfo.getGameId() != gameIdToConnectTo) {
                assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
            }
        }
    }

    /**
     * @given a running game server
     * @when a player connects to a server using wrong game id
     * @then the player is not connected
     */
    @Test
    public void testJoinGameNotExistingGame() throws IOException, InterruptedException {
        int gameIdToConnectTo = 666;
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.NOT_EXISTING_GAME_ROOM.ordinal(), errorEvent.getErrorCode(),
                "Should a non-existing game error");
        assertEquals("Not existing game room", errorEvent.getMessage());

        // need a new game connection because the previous is closed
        var newGameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        assertEquals(0, newGameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, newGameConnection.getResponse().size(), "Should be exactly one response");

        ServerResponse gamesInfoServerResponse = newGameConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = gamesInfoServerResponse.getServerInfo().getGamesList();
        assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
        }
        assertTrue(gameConnection.isDisconnected());
    }

    /**
     * @given a running game server
     * @when a player connects with older major version connects to a server
     * @then the player is not connected
     */
    @Test
    public void testJoinGameWrongVersion() throws IOException, InterruptedException {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion("0.1.1-SNAPSHOT")
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.COMMAND_NOT_RECOGNIZED.ordinal(), errorEvent.getErrorCode(),
                "Command should not be recognized as client version is too old");

        // need a new game connection because the previous is closed
        var newGameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        assertEquals(0, newGameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, newGameConnection.getResponse().size(), "Should be exactly one response");

        ServerResponse gamesInfoServerResponse = newGameConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = gamesInfoServerResponse.getServerInfo().getGamesList();
        assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
        }
        assertTrue(gameConnection.isDisconnected());
    }

    /**
     * @given a running game server with max number of players connected to game 0
     * @when one more player connects to game 0
     * @then the player is not connected as the server is full
     */
    @Test
    public void testJoinGameTooMany() throws IOException, InterruptedException {
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
            GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
            gameConnection.write(
                    JoinGameCommand.newBuilder()
                            .setVersion(ServerConfig.VERSION)
                            .setPlayerName("my player name " + i)
                            .setGameId(0).build());
        }

        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(0).build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be 1 response");

        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.SERVER_FULL.ordinal(), errorEvent.getErrorCode(),
                "Should be a server full error");
        assertEquals("Can't connect player. Server is full.", errorEvent.getMessage());
        assertTrue(gameConnection.isDisconnected());
    }

    /**
     * @given a running game server
     * @when 2 players connect with the same name
     * @then 1st player is connected, 2nd player is not
     */
    @Test
    public void testJoinSameName() throws IOException, InterruptedException {

        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("same name")
                        .setGameId(0).build());


        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("same name")
                        .setGameId(0).build());
        Thread.sleep(150);
        assertEquals(0, gameConnection2.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection2.getResponse().size(), "Should be 1 response");

        ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.PLAYER_EXISTS.ordinal(), errorEvent.getErrorCode(),
                "Shouldn't be able to connect as the player name is already taken");
        assertEquals("Can't connect player. Player name already taken.", errorEvent.getMessage());

        assertTrue(gameConnection1.isConnected());
        assertTrue(gameConnection2.isDisconnected());
    }

    /**
     * @given a running game server with MAX_PLAYERS_PER_GAME-1 players connected to game 0
     * @when a new player connects to game 0
     * @then the player is successfully connected
     */
    @Test
    public void testJoinGameAlmostFull() throws Exception {
        int gameIdToConnectTo = 0;
        Map<Integer, ServerResponse.Vector> connectedPlayersPositions = new HashMap<>();
        for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME - 1; i++) {
            GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
            gameConnection.write(
                    JoinGameCommand.newBuilder()
                            .setVersion(ServerConfig.VERSION)
                            .setPlayerName("my player name " + i)
                            .setGameId(gameIdToConnectTo).build());
            Thread.sleep(150);
            ServerResponse mySpawnResponse = gameConnection.getResponse().poll().get();
            var mySpawnEvent = mySpawnResponse.getGameEvents().getEvents(0);
            connectedPlayersPositions.put(mySpawnEvent.getPlayer().getPlayerId(), mySpawnEvent.getPlayer().getPosition());
        }
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, connectedPlayersPositions.size(),
                "All players must have unique ids. Something is off");

        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
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
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, allOtherPlayersResponse.getGameEvents().getPlayersOnline());
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME - 1, allOtherPlayersResponse.getGameEvents().getEventsCount(),
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
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, myGame.getPlayersOnline(), "We should connect all players");

        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            if (gameInfo.getGameId() != gameIdToConnectTo) {
                assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
            }
        }
    }

    /**
     * @given a running game server
     * @when max number of player connect to all games
     * @then all players are successfully connected
     */
    @Test
    public void testJoinGameMaxPlayersAllGames() throws Exception {
        for (int gameId = 0; gameId < ServerConfig.GAMES_TO_CREATE; gameId++) {
            for (int j = 0; j < ServerConfig.MAX_PLAYERS_PER_GAME; j++) {
                GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
                gameConnection.write(
                        JoinGameCommand.newBuilder()
                                .setVersion(ServerConfig.VERSION)
                                .setPlayerName("my player name " + j)
                                .setGameId(gameId).build());
                Thread.sleep(150);
                assertTrue(gameConnection.isConnected());
            }
        }

        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getPlayersOnline(), "All players are connected");
        }
    }
}
