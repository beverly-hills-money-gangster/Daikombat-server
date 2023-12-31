package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class GameServerTest {

    private int port;
    private ServerRunner serverRunner;

    private final List<GameConnection> gameConnections = new ArrayList<>();


    @BeforeEach
    public void setUp() throws InterruptedException {
        port = ThreadLocalRandom.current().nextInt(49_151, 65_535);
        serverRunner = new ServerRunner(port);
        new Thread(() -> {
            try {
                serverRunner.runServer();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        serverRunner.waitFullyRunning();
    }

    @AfterEach
    public void tearDown() {
        serverRunner.stop();
        for (GameConnection gameConnection : gameConnections) {
            Optional.ofNullable(gameConnection).ifPresent(GameConnection::disconnect);
        }
    }

    @Test
    public void testGetServerInfo() throws InterruptedException, IOException {
        GameConnection gameConnection = new GameConnection(GameServerCreds.builder()
                .password(GameConfig.PASSWORD)
                .hostPort(HostPort.builder().host("localhost").port(port).build())
                .build());
        gameConnections.add(gameConnection);
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(1_500);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(GameConfig.GAMES_TO_CREATE, games.size());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
        }
    }

    @Test
    public void testGetServerInfoBadAuth() throws InterruptedException, IOException {
        GameConnection gameConnection = new GameConnection(GameServerCreds.builder()
                .password("wrong password")
                .hostPort(HostPort.builder().host("localhost").port(port).build())
                .build());
        gameConnections.add(gameConnection);
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(1_500);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
        assertEquals(GameErrorCode.AUTH_ERROR.ordinal(), errorEvent.getErrorCode(), "Should be auth error");
        assertEquals("Invalid HMAC", errorEvent.getMessage());
    }

    @Test
    public void testGetServerInfoNotExistingServer() {
        assertThrows(IOException.class, () -> new GameConnection(GameServerCreds.builder()
                .password(GameConfig.PASSWORD)
                .hostPort(HostPort.builder().host("192.168.0.25").port(port).build())
                .build()));
    }

    @Test
    public void testGetServerInfoWrongPort() {
        assertThrows(IOException.class, () -> new GameConnection(GameServerCreds.builder()
                .password(GameConfig.PASSWORD)
                .hostPort(HostPort.builder().host("localhost").port(666).build())
                .build()));
    }

    @Test
    public void testJoinGame() throws IOException, InterruptedException {
        GameConnection gameConnection = new GameConnection(GameServerCreds.builder()
                .password(GameConfig.PASSWORD)
                .hostPort(HostPort.builder().host("localhost").port(port).build())
                .build());
        gameConnections.add(gameConnection);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(0).build());
        Thread.sleep(1_500);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(2, gameConnection.getResponse().size(),
                "Should be exactly 2 responses: my spawn + all players");

        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        assertEquals(1, mySpawn.getGameEvents().getEventsCount(), "Should be only my spawn");
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        assertEquals("my player name", mySpawnGameEvent.getPlayer().getPlayerName());
        assertEquals(100, mySpawnGameEvent.getPlayer().getHealth());


        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(1_500);
        assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
        assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        assertEquals(GameConfig.GAMES_TO_CREATE, games.size());
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(GameConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
            if (gameInfo.getGameId() == 0) {
                assertEquals(1, gameInfo.getPlayersOnline(), "Should be 1 connected player only");
            } else {
                assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
            }
        }

    }


    @Test
    public void testJoinGameNotExistingGame() {

    }


    @Test
    public void testJoinGameMultiple() {

    }

    @Test
    public void testJoinGameConcurrency() {

    }

    // TODO finish it
    /*
    Test add joinGame for multiple players
    Test all commands
    Test auth (positive and negative cases)
    Test concurrent access
    Test error handling
     */
}
