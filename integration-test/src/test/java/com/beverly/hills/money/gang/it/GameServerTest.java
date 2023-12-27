package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.GameConfig;
import com.beverly.hills.money.gang.entity.GameServerCreds;
import com.beverly.hills.money.gang.entity.HostPort;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.runner.ServerRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameServerTest {

    private int port;
    private ServerRunner serverRunner;

    private GameConnection gameConnection;


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
        Optional.ofNullable(gameConnection).ifPresent(GameConnection::disconnect);
    }

    @Test
    public void testGetServerInfo() throws InterruptedException {
        gameConnection = new GameConnection(GameServerCreds.builder()
                .password(GameConfig.PASSWORD)
                .hostPort(HostPort.builder().host("localhost").port(port).build())
                .build());
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(5_000);
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

    // TODO finish it
    /*
    Test all commands
    Test auth (positive and negative cases)
    Test concurrent access
    Test error handling
     */
}
