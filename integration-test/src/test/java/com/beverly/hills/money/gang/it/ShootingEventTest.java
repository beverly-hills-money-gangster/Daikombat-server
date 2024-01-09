package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SetEnvironmentVariable(key = "MOVES_UPDATE_FREQUENCY_MLS", value = "9999")
public class ShootingEventTest extends AbstractGameServerTest {

    @Test
    public void testShootMiss() throws IOException, InterruptedException {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        emptyQueue(gameConnection2.getResponse());
        ServerResponse mySpawn = gameConnection1.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
        var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
        float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setPlayerId(playerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(mySpawnEvent.getPlayer().getDirection().getX())
                                .setY(mySpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .build());
        Thread.sleep(250);
        assertEquals(1, gameConnection2.getResponse().size(), "Only 1(shooting) event is expected");
        ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
        assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
        assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One shooting event is expected");
        assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(), "2 players are connected now");
        var shootingEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.SHOOT, shootingEvent.getEventType());
        assertEquals(playerId, shootingEvent.getPlayer().getPlayerId());

        assertEquals(mySpawnEvent.getPlayer().getDirection().getX(), shootingEvent.getPlayer().getDirection().getX(),
                "Direction shouldn't change");
        assertEquals(mySpawnEvent.getPlayer().getDirection().getY(), shootingEvent.getPlayer().getDirection().getY(),
                "Direction shouldn't change");
        assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
        assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

        assertEquals(100, shootingEvent.getPlayer().getHealth(), "Full health is nobody got shot(miss)");
        assertFalse(shootingEvent.hasAffectedPlayer(), "Nobody is affected. Missed the shot");
    }

    @Test
    public void testShootHit() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        emptyQueue(gameConnection2.getResponse());

        ServerResponse shooterPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        ServerResponse shotPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setPlayerId(shooterPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                                .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .setAffectedPlayerId(shotPlayerId)
                .build());
        Thread.sleep(250);
        assertEquals(1, gameConnection2.getResponse().size(), "Only 1(shooting) event is expected");
        ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
        assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
        assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One shooting event is expected");
        assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(), "2 players are connected now");
        var shootingEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.GET_SHOT, shootingEvent.getEventType());
        assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

        assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(), shootingEvent.getPlayer().getDirection().getX(),
                "Direction shouldn't change");
        assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(), shootingEvent.getPlayer().getDirection().getY(),
                "Direction shouldn't change");
        assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
        assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

        assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
        assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
        assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
        assertEquals(100 - ServerConfig.DEFAULT_DAMAGE, shootingEvent.getAffectedPlayer().getHealth());
        assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
                shootingEvent.getAffectedPlayer().getPosition().getX(),
                "Shot player hasn't moved so position has to stay the same");
        assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
                shootingEvent.getAffectedPlayer().getPosition().getY(),
                "Shot player hasn't moved so position has to stay the same");

        emptyQueue(gameConnection1.getResponse());
        gameConnection1.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(150);
        var serverInfoResponse = gameConnection1.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
    }

    @Test
    public void testShootKill() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        emptyQueue(gameConnection2.getResponse());

        ServerResponse shooterPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        ServerResponse shotPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_DAMAGE);
        for (int i = 0; i < shotsToKill - 1; i++) {
            gameConnection1.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(shooterPlayerId)
                    .setGameId(gameIdToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                    .setDirection(
                            PushGameEventCommand.Vector.newBuilder()
                                    .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                                    .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                                    .build())
                    .setPosition(
                            PushGameEventCommand.Vector.newBuilder()
                                    .setX(newPositionX)
                                    .setY(newPositionY)
                                    .build())
                    .setAffectedPlayerId(shotPlayerId)
                    .build());
            Thread.sleep(250);
            ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
            var shootingEvent = serverResponse.getGameEvents().getEvents(0);
            assertEquals(ServerResponse.GameEvent.GameEventType.GET_SHOT, shootingEvent.getEventType());
            assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
            assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
            assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
            assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
            assertEquals(100 - ServerConfig.DEFAULT_DAMAGE * (i + 1), shootingEvent.getAffectedPlayer().getHealth());
        }
        // this one kills player 2
        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setPlayerId(shooterPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                                .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .setAffectedPlayerId(shotPlayerId)
                .build());
        Thread.sleep(250);
        ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
        var shootingEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.DEATH, shootingEvent.getEventType(),
                "Shot player must be dead");
        assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
        assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
        assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
        assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
        assertEquals(0, shootingEvent.getAffectedPlayer().getHealth());

        emptyQueue(gameConnection1.getResponse());
        gameConnection1.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(250);
        var serverInfoResponse = gameConnection1.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "Must be 1 player only as 1 player got killed (it was 2)");
    }

    @Test
    public void testShootDeadPlayer() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        emptyQueue(gameConnection2.getResponse());

        ServerResponse shooterPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        ServerResponse shotPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_DAMAGE);
        for (int i = 0; i < shotsToKill; i++) {
            gameConnection1.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(shooterPlayerId)
                    .setGameId(gameIdToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                    .setDirection(
                            PushGameEventCommand.Vector.newBuilder()
                                    .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                                    .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                                    .build())
                    .setPosition(
                            PushGameEventCommand.Vector.newBuilder()
                                    .setX(newPositionX)
                                    .setY(newPositionY)
                                    .build())
                    .setAffectedPlayerId(shotPlayerId)
                    .build());
            Thread.sleep(250);
            ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
            var shootingEvent = serverResponse.getGameEvents().getEvents(0);
            if (i == shotsToKill - 1) {
                // last shot is a kill
                assertEquals(ServerResponse.GameEvent.GameEventType.DEATH, shootingEvent.getEventType());
            } else {
                assertEquals(ServerResponse.GameEvent.GameEventType.GET_SHOT, shootingEvent.getEventType());
            }
            assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
            assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
            assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
            assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
            assertEquals(100 - ServerConfig.DEFAULT_DAMAGE * (i + 1), shootingEvent.getAffectedPlayer().getHealth());
        }
        emptyQueue(gameConnection1.getResponse());
        // shoot dead player
        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setPlayerId(shooterPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                                .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .setAffectedPlayerId(shotPlayerId)
                .build());
        Thread.sleep(250);
        assertEquals(0, gameConnection1.getResponse().size(),
                "Should be no response as you can't shoot a dead player");
        assertEquals(0, gameConnection1.getWarning().size(),
                "Should be no warnings");
        assertEquals(0, gameConnection1.getErrors().size(),
                "Should be no errors as this situation might happen in a fast paced game");

        emptyQueue(gameConnection1.getResponse());
        gameConnection1.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(250);
        var serverInfoResponse = gameConnection1.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "Must be 1 player only as 1 player got killed (it was 2)");
    }

    @Test
    public void testShootYourself() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        Thread.sleep(150);
        ServerResponse shooterPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(gameConnection1.getResponse());


        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;

        gameConnection1.write(PushGameEventCommand.newBuilder()
                .setPlayerId(shooterPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.SHOOT)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                                .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .setAffectedPlayerId(shooterPlayerId) // shoot yourself
                .build());

        Thread.sleep(250);
        assertEquals(1, gameConnection1.getResponse().size(), "Should be one error response");

        ServerResponse errorResponse = gameConnection1.getResponse().poll().get();
        assertTrue(errorResponse.hasErrorEvent());
        ServerResponse.ErrorEvent errorEvent = errorResponse.getErrorEvent();
        assertEquals(GameErrorCode.CAN_NOT_SHOOT_YOURSELF.ordinal(), errorEvent.getErrorCode(),
                "You can't shoot yourself");

        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        Thread.sleep(50);
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(0, gameInfo.getPlayersOnline(),
                    "Should be no connected players anywhere. The only player on server got disconnected");
        }
    }
}
