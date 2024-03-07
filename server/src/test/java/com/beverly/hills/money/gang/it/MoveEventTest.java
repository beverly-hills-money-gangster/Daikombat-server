package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "250")
public class MoveEventTest extends AbstractGameServerTest {

    /**
     * @given a running server with 2 connected players
     * @when player 1 moves, player 2 observes
     * @then player 2 observers player 1 moves
     */
    @Test
    public void testMove() throws Exception {
        int gameIdToConnectTo = 2;
        GameConnection movingPlayerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        movingPlayerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(movingPlayerConnection.getResponse());
        ServerResponse mySpawn = movingPlayerConnection.getResponse().poll().get();
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

        GameConnection observerPlayerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerPlayerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("new player")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
        emptyQueue(observerPlayerConnection.getResponse());
        Thread.sleep(1_000);
        assertEquals(0, observerPlayerConnection.getResponse().size(),
                "No activity happened in the game so no response yet. Actual response is " + observerPlayerConnection.getResponse().list());

        float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
        float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
        emptyQueue(movingPlayerConnection.getResponse());
        movingPlayerConnection.write(PushGameEventCommand.newBuilder()
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
        assertEquals(0, movingPlayerConnection.getResponse().size(),
                "Moving player is not expected to get any events. Moving player doesn't receive his own moves.");
        assertEquals(1, observerPlayerConnection.getResponse().size(),
                "Only one response is expected(player 1 move)");

        ServerResponse moveServerResponse = observerPlayerConnection.getResponse().poll().get();
        assertEquals(2, moveServerResponse.getGameEvents().getPlayersOnline());
        assertTrue(moveServerResponse.hasGameEvents(), "Should be a game event");
        assertEquals(1, moveServerResponse.getGameEvents().getEventsCount(),
                "Only one game even is expected(player 1 move)");
        ServerResponse.GameEvent player1MoveEvent = moveServerResponse.getGameEvents().getEvents(0);

        assertEquals(playerId1, player1MoveEvent.getPlayer().getPlayerId(), "Should be player 1 id");
        assertEquals(ServerResponse.GameEvent.GameEventType.MOVE, player1MoveEvent.getEventType());
        assertFalse(player1MoveEvent.hasLeaderBoard(), "We shouldn't receive leader boards on moves");
        assertEquals(mySpawnGameEvent.getPlayer().getDirection().getX(), player1MoveEvent.getPlayer().getDirection().getX(),
                0.00001, "Direction should not change");
        assertEquals(mySpawnGameEvent.getPlayer().getDirection().getY(), player1MoveEvent.getPlayer().getDirection().getY(),
                0.00001, "Direction should not change");

        assertEquals(newPositionX, player1MoveEvent.getPlayer().getPosition().getX(),
                0.00001);
        assertEquals(newPositionY, player1MoveEvent.getPlayer().getPosition().getY(),
                0.00001);

        Thread.sleep(1_000);
        assertEquals(0, observerPlayerConnection.getResponse().size(),
                "No action so no response is expected");
    }

    /**
     * @given a running server with 2 connected players
     * @when player 1 move too fast, player 2 observes
     * @then player 1 is disconnected, player 2 sees player exit
     */
    @Test
    @Disabled("Enable when anti-cheat is ready")
    public void testMoveTooFast() throws Exception {
        int gameIdToConnectTo = 2;
        GameConnection cheatingPlayerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        cheatingPlayerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(cheatingPlayerConnection.getResponse());
        ServerResponse mySpawn = cheatingPlayerConnection.getResponse().poll().get();
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

        GameConnection observerPlayerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerPlayerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("new player")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
        emptyQueue(observerPlayerConnection.getResponse());
        Thread.sleep(1_000);
        assertEquals(0, observerPlayerConnection.getResponse().size(),
                "No activity happened in the game so no response yet. Actual response is " + observerPlayerConnection.getResponse().list());

        // moving too fast
        float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 10f;
        float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 10f;
        emptyQueue(cheatingPlayerConnection.getResponse());
        cheatingPlayerConnection.write(PushGameEventCommand.newBuilder()
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
        assertTrue(cheatingPlayerConnection.isDisconnected(), "Cheating player should be disconnected");
        assertTrue(observerPlayerConnection.isConnected());


        ServerResponse exitServerResponse = observerPlayerConnection.getResponse().poll().get();
        assertEquals(1, exitServerResponse.getGameEvents().getPlayersOnline(),
                "Only 1 player is expected to be online now. Cheating player should exit.");
        assertTrue(exitServerResponse.hasGameEvents(), "Should be a game event");
        assertEquals(1, exitServerResponse.getGameEvents().getEventsCount(),
                "Only one game even is expected(player 1 exit)");
        ServerResponse.GameEvent playerExitEvent = exitServerResponse.getGameEvents().getEvents(0);

        assertEquals(playerId1, playerExitEvent.getPlayer().getPlayerId(), "Should be player 1 id");
        assertEquals(ServerResponse.GameEvent.GameEventType.EXIT, playerExitEvent.getEventType());
    }

    /**
     * @given a running server with 2 connected players
     * @when player 2 uses player 1 id to move
     * @then player 1 move is not published
     */
    @Test
    public void testMoveWrongPlayerId() throws Exception {
        int gameIdToConnectTo = 2;
        GameConnection observerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(observerConnection.getResponse());
        ServerResponse mySpawn = observerConnection.getResponse().poll().get();
        ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
        int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

        GameConnection wrongGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        wrongGameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("new player")
                        .setGameId(gameIdToConnectTo).build());

        waitUntilQueueNonEmpty(wrongGameConnection.getResponse());
        waitUntilQueueNonEmpty(observerConnection.getResponse());
        emptyQueue(wrongGameConnection.getResponse());
        emptyQueue(observerConnection.getResponse());
        Thread.sleep(1_000);
        assertEquals(0, wrongGameConnection.getResponse().size(),
                "No activity happened in the game so no response yet. Actual response is " + wrongGameConnection.getResponse().list());

        float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
        float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;

        // wrong game connection
        wrongGameConnection.write(PushGameEventCommand.newBuilder()
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

        Thread.sleep(250);

        assertTrue(wrongGameConnection.isConnected());
        assertTrue(observerConnection.isConnected());
        assertEquals(0, observerConnection.getResponse().size(),
                "No movements should be published. Actual:" + observerConnection.getResponse().list());
        assertEquals(0, wrongGameConnection.getResponse().size(),
                "No movements should be published. Actual:" + wrongGameConnection.getResponse().list());

    }

}
