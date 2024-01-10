package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SetEnvironmentVariable(key = "MOVES_UPDATE_FREQUENCY_MLS", value = "9999")
public class ExitEventTest extends AbstractGameServerTest {

    @Test
    public void testExit() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);
        ServerResponse mySpawn = gameConnection1.getResponse().poll().get();
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my other player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);

        emptyQueue(gameConnection1.getResponse());
        emptyQueue(gameConnection2.getResponse());
        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setGameId(gameToConnectTo)
                .setPlayerId(playerId1)
                .setEventType(PushGameEventCommand.GameEventType.EXIT)
                .build());
        Thread.sleep(150);
        assertTrue(gameConnection1.isDisconnected(), "Player 1 should be disconnected now");
        assertTrue(gameConnection2.isConnected(), "Player 2 should be connected");

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
}
