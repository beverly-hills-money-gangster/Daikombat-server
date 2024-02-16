package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ClientConfig;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SetEnvironmentVariable(key = "GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS", value = "1000")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "1000")
public class IdleClientTest extends AbstractGameServerTest {

    /**
     * @given a running game server and 2 connected players(idle and active/observer)
     * @when active/observer player moves and idle player does nothing for long time
     * @then idle player gets disconnected and active/observer player gets EXIT event for the idle player
     */
    @Test
    public void testIdleClientDisconnect() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnectionIdle = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        GameConnection gameConnectionObserver = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        gameConnectionIdle.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ClientConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        gameConnectionObserver.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ClientConfig.VERSION)
                        .setPlayerName("my player name observer")
                        .setGameId(gameToConnectTo).build());
        waitUntilQueueNonEmpty(gameConnectionIdle.getResponse());
        waitUntilQueueNonEmpty(gameConnectionObserver.getResponse());

        ServerResponse idleSpawn = gameConnectionIdle.getResponse().poll().get();
        ServerResponse.GameEvent idleSpawnGameEvent = idleSpawn.getGameEvents().getEvents(0);
        int idlePlayerId = idleSpawnGameEvent.getPlayer().getPlayerId();

        ServerResponse observerSpawn = gameConnectionObserver.getResponse().poll().get();
        ServerResponse.GameEvent observerSpawnGameEvent = observerSpawn.getGameEvents().getEvents(0);
        int observerPlayerId = observerSpawnGameEvent.getPlayer().getPlayerId();

        Thread.sleep(1_000);
        emptyQueue(gameConnectionObserver.getResponse());
        emptyQueue(gameConnectionIdle.getResponse());

        gameConnectionIdle.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(gameConnectionIdle.getResponse());
        ServerResponse gameServerInfoResponse = gameConnectionIdle.getResponse().poll().get();
        var myGame = gameServerInfoResponse.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + gameServerInfoResponse));

        assertEquals(2, myGame.getPlayersOnline(), "2 players should be connected(idle player + observer)");


        float newPositionY = observerSpawnGameEvent.getPlayer().getPosition().getY();
        float newPositionX = observerSpawnGameEvent.getPlayer().getPosition().getX();

        int observerMoves = 50;
        // move observer, idle player does nothing meanwhile
        for (int i = 0; i < observerMoves; i++) {
            newPositionY += 0.1f;
            newPositionX += 0.1f;
            gameConnectionObserver.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(observerPlayerId)
                    .setGameId(gameToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.MOVE)
                    .setDirection(PushGameEventCommand.Vector.newBuilder().setX(0).setY(1).build())
                    .setPosition(PushGameEventCommand.Vector.newBuilder()
                            .setX(newPositionX).setY(newPositionY).build())
                    .build());
            Thread.sleep(200);
        }

        GameConnection newGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(newGameConnection.getResponse());
        ServerResponse serverResponseAfterIdle = newGameConnection.getResponse().poll().get();
        var myGameAfterIdle = serverResponseAfterIdle.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterIdle));

        assertEquals(1, myGameAfterIdle.getPlayersOnline(),
                "Idle player should be disconnected because it was idle for too long. Only observer player is online");

        emptyQueue(gameConnectionIdle.getWarning());
        gameConnectionIdle.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(250);
        assertEquals(1, gameConnectionIdle.getWarning().size(),
                "Should be one warning as we can't write using disconnected connection");
        Throwable warning = gameConnectionIdle.getWarning().poll().get();
        assertTrue(warning instanceof IOException);
        assertEquals("Can't write using closed connection", warning.getMessage());
        assertTrue(gameConnectionIdle.isDisconnected());

        assertTrue(gameConnectionObserver.isConnected(), "Observer should still be connected. It wasn't idle");

        boolean validExitEventExists = gameConnectionObserver.getResponse().list().stream()
                .filter(ServerResponse::hasGameEvents)
                .map(ServerResponse::getGameEvents)
                .anyMatch(gameEvents -> gameEvents.getEventsCount() == 1
                        && Optional.of(gameEvents.getEvents(0))
                        .map(gameEvent ->
                                gameEvent.getEventType() == ServerResponse.GameEvent.GameEventType.EXIT
                                        && gameEvent.getPlayer().getPlayerId() == idlePlayerId).orElse(false));
        assertTrue(validExitEventExists, "There must be a valid EXIT event for idle player. " +
                "Actual response is :" + gameConnectionObserver.getResponse().list());

        assertEquals(2, gameConnectionIdle.getNetworkStats().getSentMessages(),
                "2 messages should be sent: join and get server info");
        assertEquals(1 + observerMoves, gameConnectionObserver.getNetworkStats().getSentMessages());
    }


    /**
     * @given a running game server and 1 connected player
     * @when the player moves
     * @then nobody gets disconnected
     */
    @Test
    public void testNotIdleClientMoving() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ClientConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        var mySpawn = gameConnection.getResponse().poll().get().getGameEvents().getEvents(0);
        int playerId = mySpawn.getPlayer().getPlayerId();

        GameConnection observerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(observerConnection.getResponse());
        ServerResponse serverResponse = observerConnection.getResponse().poll().get();
        var myGame = serverResponse.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponse));

        assertEquals(1, myGame.getPlayersOnline(), "Only the current player should be connected");

        float newPositionY = mySpawn.getPlayer().getPosition().getY();
        float newPositionX = mySpawn.getPlayer().getPosition().getX();
        // move
        for (int i = 0; i < 50; i++) {
            newPositionY += 0.1f;
            newPositionX += 0.1f;
            gameConnection.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(playerId)
                    .setGameId(gameToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.MOVE)
                    .setDirection(PushGameEventCommand.Vector.newBuilder().setX(0).setY(1).build())
                    .setPosition(PushGameEventCommand.Vector.newBuilder()
                            .setX(newPositionX).setY(newPositionY).build())
                    .build());
            Thread.sleep(200);
        }
        assertTrue(gameConnection.isConnected());

        GameConnection newGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(newGameConnection.getResponse());
        ServerResponse serverResponseAfterMoving = newGameConnection.getResponse().poll().get();
        var myGameAfterMoving = serverResponseAfterMoving.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterMoving));

        assertEquals(1, myGameAfterMoving.getPlayersOnline(),
                "Current player should be kept online(not disconnected due to idleness)");

    }

    /**
     * @given a running game server and 1 connected player
     * @when the player pings
     * @then nobody gets disconnected
     */
    @Test
    public void testNotIdleClientPing() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ClientConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        var myGame = serverResponse.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponse));

        assertEquals(1, myGame.getPlayersOnline(), "Only the current player should be connected");
        assertTrue(gameConnection.isConnected());

        // move
        for (int i = 0; i < 50; i++) {

            gameConnection.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(playerId)
                    .setGameId(gameToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.PING)
                    .build());
            Thread.sleep(200);
        }
        assertTrue(gameConnection.isConnected());

        GameConnection newGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(newGameConnection.getResponse());
        ServerResponse serverResponseAfterPinging = newGameConnection.getResponse().poll().get();
        var myGameAfterPinging = serverResponseAfterPinging.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterPinging));

        assertEquals(1, myGameAfterPinging.getPlayersOnline(),
                "Current player should be kept online(not disconnected due to idleness)");

    }

    /**
     * @given a running game server and 1 connected player
     * @when the player moves and then stops doing anything for long time
     * @then the player gets disconnected
     */
    @Test
    public void testMoveThenIdleClient() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ClientConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(gameConnection.getResponse());
        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        var myGame = serverResponse.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponse));

        assertEquals(1, myGame.getPlayersOnline(), "Only the current player should be connected");

        // move
        for (int i = 0; i < 50; i++) {

            gameConnection.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(playerId)
                    .setGameId(gameToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.MOVE)
                    .setDirection(PushGameEventCommand.Vector.newBuilder().setX(0).setY(1).build())
                    .setPosition(PushGameEventCommand.Vector.newBuilder().setX(i).setY(i).build())
                    .build());
            Thread.sleep(200);
        }
        // do nothing
        Thread.sleep(10_000);

        assertTrue(gameConnection.isDisconnected());
        GameConnection newGameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(newGameConnection.getResponse());
        ServerResponse serverResponseAfterMoving = newGameConnection.getResponse().poll().get();
        var myGameAfterMoving = serverResponseAfterMoving.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterMoving));

        assertEquals(0, myGameAfterMoving.getPlayersOnline(),
                "Should be disconnected because it was idle for too long even though it moved in the beginning");

    }
}
