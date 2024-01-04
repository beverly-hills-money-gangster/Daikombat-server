package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SetEnvironmentVariable(key = "IDLE_PLAYERS_KILLER_FREQUENCY_MLS", value = "1000")
@SetEnvironmentVariable(key = "MAX_IDLE_TIME_MLS", value = "1000")
public class IdleClientTest extends AbstractGameServerTest {

    @Test
    public void testIdleClientDisconnect() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);
        emptyQueue(gameConnection.getResponse());

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        var myGame = serverResponse.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponse));

        assertEquals(1, myGame.getPlayersOnline(), "Only the current player should be connected");

        // idle for long time
        Thread.sleep(10_000);

        GameConnection newGameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        ServerResponse serverResponseAfterIdle = newGameConnection.getResponse().poll().get();
        var myGameAfterIdle = serverResponseAfterIdle.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterIdle));

        assertEquals(0, myGameAfterIdle.getPlayersOnline(),
                "Current player should be disconnected because it was idle for too long");

    }

    @Test
    public void testNotIdleClient() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);
        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
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

        GameConnection newGameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        ServerResponse serverResponseAfterMoving = newGameConnection.getResponse().poll().get();
        var myGameAfterMoving = serverResponseAfterMoving.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterMoving));

        assertEquals(1, myGameAfterMoving.getPlayersOnline(),
                "Current player should be kept online(not disconnected due to idleness)");

    }

    @Test
    public void testMoveThenIdleClient() throws IOException, InterruptedException {
        int gameToConnectTo = 1;
        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameToConnectTo).build());
        Thread.sleep(150);
        ServerResponse mySpawn = gameConnection.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
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

        GameConnection newGameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        newGameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        ServerResponse serverResponseAfterMoving = newGameConnection.getResponse().poll().get();
        var myGameAfterMoving = serverResponseAfterMoving.getServerInfo().getGamesList().stream().filter(gameInfo
                        -> gameInfo.getGameId() == gameToConnectTo).findFirst()
                .orElseThrow(() -> new IllegalStateException("Can't find game by id. Response is:" + serverResponseAfterMoving));

        assertEquals(0, myGameAfterMoving.getPlayersOnline(),
                "Should be disconnected because it was idle for too long even though it moved in the beginning");

    }
}
