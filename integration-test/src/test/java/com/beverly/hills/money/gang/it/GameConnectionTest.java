package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SetEnvironmentVariable(key = "MOVES_UPDATE_FREQUENCY_MLS", value = "9999")
public class GameConnectionTest extends AbstractGameServerTest {

    /**
     * @given a running game server with 2 connected players
     * @when player 1 disconnects from server
     * @then player 1 gets disconnected and player 2 has the even DISCONNECT for player 1
     */
    @Test
    public void testExit() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);
        ServerResponse mySpawn = gameConnection1.getResponse().poll().get();
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);

        emptyQueue(gameConnection1.getResponse());
        emptyQueue(gameConnection2.getResponse());
        gameConnection1.disconnect();
        Thread.sleep(150);
        assertTrue(gameConnection1.isDisconnected(), "Player 1 should be disconnected now");
        assertTrue(gameConnection2.isConnected(), "Player 2 should be connected");

        gameConnection1.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        assertEquals(0, gameConnection1.getResponse().size(),
                "Should be no response because the connection is closed");
        assertEquals(1, gameConnection1.getWarning().size(),
                "Should be one warning because the connection is closed");
        Throwable error = gameConnection1.getWarning().poll().get();
        assertEquals(IOException.class, error.getClass());
        assertEquals("Can't write using closed connection", error.getMessage());


        assertEquals(1,
                gameConnection2.getResponse().size(), "We need to get 1 response(EXIT)");
        ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
        assertTrue(serverResponse.hasGameEvents());
        assertEquals(1, serverResponse.getGameEvents().getEventsCount());
        var disconnectEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(playerId1, disconnectEvent.getPlayer().getPlayerId());
        assertEquals(ServerResponse.GameEvent.GameEventType.EXIT, disconnectEvent.getEventType());
        assertEquals(1, serverResponse.getGameEvents().getPlayersOnline(),
                "1 player left because the other disconnected himself");
    }

    /**
     * @given a running game server with 1 connected player
     * @when player 1 disconnects from server twice
     * @then player 1 gets disconnected. 2nd disconnect attempt does not cause any issues
     */
    @Test
    public void testDisconnectTwice() throws IOException, InterruptedException {
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        assertTrue(gameConnection.isConnected(), "Should be connected by default");
        assertFalse(gameConnection.isDisconnected());
        gameConnection.disconnect();
        gameConnection.disconnect(); // call twice
        assertTrue(gameConnection.isDisconnected(), "Should be disconnected after disconnecting");
        assertFalse(gameConnection.isConnected());
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        assertEquals(0, gameConnection.getResponse().size(),
                "Should be no response because the connection is closed");
        assertEquals(1, gameConnection.getWarning().size(),
                "Should be one warning because the connection is closed");
        Throwable error = gameConnection.getWarning().poll().get();
        assertEquals(IOException.class, error.getClass());
        assertEquals("Can't write using closed connection", error.getMessage());
    }

    /**
     * @given a running game server
     * @when player 1 connects to a non-existing host
     * @then an exception is thrown
     */
    @Test
    public void testGetServerInfoNotExistingServer() {
        assertThrows(IOException.class, () -> createGameConnection(ServerConfig.PASSWORD, "666.666.666.666", port));
    }

    /**
     * @given a running game server
     * @when player 1 connects to correct host but incorrect port
     * @then an exception is thrown
     */
    @Test
    public void testGetServerInfoWrongPort() {
        assertThrows(IOException.class, () -> createGameConnection(ServerConfig.PASSWORD, "localhost", 666));
    }
}
