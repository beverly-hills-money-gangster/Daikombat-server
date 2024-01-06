package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

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
    public void testShootHit() throws IOException, InterruptedException {
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

        ServerResponse shotPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        ServerResponse shooterPlayerSpawn = gameConnection1.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

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

        // TODO finish it. Check shot player position and direction
    }

    @Test
    public void testShootKill() {

    }
}
