package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SetEnvironmentVariable(key = "MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
public class ClientDisconnectTest extends AbstractGameServerTest {


    @Test
    public void testDisconnect() throws IOException, InterruptedException {
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        assertTrue(gameConnection.isConnected(), "Should be connected by default");
        assertFalse(gameConnection.isDisconnected());
        gameConnection.disconnect();
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
}
