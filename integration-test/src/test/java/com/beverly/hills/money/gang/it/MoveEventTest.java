package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SetEnvironmentVariable(key = "MOVES_UPDATE_FREQUENCY_MLS", value = "250")
public class MoveEventTest extends AbstractGameServerTest {

    @Test
    public void testMove() throws Exception {
        int gameIdToConnectTo = 2;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        ServerResponse mySpawn = gameConnection1.getResponse().poll().get();
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("new player")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        emptyQueue(gameConnection2.getResponse());
        Thread.sleep(1_000);
        assertEquals(0, gameConnection2.getResponse().size(),
                "No activity happened in the game so no response yet. Actual response is " + gameConnection2.getResponse().list());

        float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
        float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.MOVE)
                .setPlayerId(playerId1)
                .setPosition(PushGameEventCommand.Vector.newBuilder()
                        .setY(newPositionY)
                        .setX(newPositionX)
                        .build())
                .setDirection(PushGameEventCommand.Vector.newBuilder()
                        .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
                        .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
                        .build())
                .build());

        Thread.sleep(1_000);
        assertEquals(1, gameConnection2.getResponse().size(),
                "Only one response is expected(player 1 move)");

        ServerResponse moveServerResponse = gameConnection2.getResponse().poll().get();
        assertEquals(2, moveServerResponse.getGameEvents().getPlayersOnline());
        assertTrue(moveServerResponse.hasGameEvents(), "Should be a game event");
        assertEquals(1, moveServerResponse.getGameEvents().getEventsCount(),
                "Only one game even is expected(player 1 move)");
        ServerResponse.GameEvent player1MoveEvent = moveServerResponse.getGameEvents().getEvents(0);

        assertEquals(playerId1, player1MoveEvent.getPlayer().getPlayerId(), "Should be player 1 id");
        assertEquals(ServerResponse.GameEvent.GameEventType.MOVE, player1MoveEvent.getEventType());
        assertEquals(100, player1MoveEvent.getPlayer().getHealth(),
                "Health should be 100% as nobody got hit");
        assertEquals(mySpawnGameEvent.getPlayer().getDirection().getX(), player1MoveEvent.getPlayer().getDirection().getX(),
                0.00001, "Direction should not change");
        assertEquals(mySpawnGameEvent.getPlayer().getDirection().getY(), player1MoveEvent.getPlayer().getDirection().getY(),
                0.00001, "Direction should not change");

        assertEquals(newPositionX, player1MoveEvent.getPlayer().getPosition().getX(),
                0.00001);
        assertEquals(newPositionY, player1MoveEvent.getPlayer().getPosition().getY(),
                0.00001);

        Thread.sleep(1_000);
        assertEquals(0, gameConnection2.getResponse().size(),
                "No action so no response is expected");
    }

}
