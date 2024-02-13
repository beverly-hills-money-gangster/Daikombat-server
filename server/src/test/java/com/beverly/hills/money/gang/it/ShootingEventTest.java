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

@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "9999")
@SetEnvironmentVariable(key = "GAME_SERVER_PING_FREQUENCY_MLS", value = "99999")
public class ShootingEventTest extends AbstractGameServerTest {

    /**
     * @given a running server with 1 connected player
     * @when player 1 shoots and misses
     * @then nobody got shot
     */
    @Test
    public void testShootMiss() throws IOException, InterruptedException {
        int gameIdToConnectTo = 0;
        GameConnection shooterConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        shooterConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection observerConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        observerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(shooterConnection.getResponse());
        waitUntilQueueNonEmpty(observerConnection.getResponse());
        emptyQueue(observerConnection.getResponse());
        ServerResponse mySpawn = shooterConnection.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
        var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
        float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        emptyQueue(shooterConnection.getResponse());
        shooterConnection.write(PushGameEventCommand.newBuilder()
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
        waitUntilQueueNonEmpty(observerConnection.getResponse());
        assertEquals(1, observerConnection.getResponse().size(), "Only 1(shooting) event is expected");
        ServerResponse serverResponse = observerConnection.getResponse().poll().get();
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
        assertEquals(2, shooterConnection.getNetworkStats().getSentMessages(),
                "Only 2 messages must be sent by shooter: join + shoot");
        assertTrue(shooterConnection.getResponse().list().isEmpty(), "Shooter shouldn't receive any new messages");
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 shoots player 2
     * @then player 2 health is reduced by ServerConfig.DEFAULT_DAMAGE and the event is sent to all players
     */
    @Test
    public void testShootHit() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection shooterConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        shooterConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection getShotConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        getShotConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(shooterConnection.getResponse());
        waitUntilQueueNonEmpty(getShotConnection.getResponse());
        emptyQueue(getShotConnection.getResponse());

        ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();


        ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        emptyQueue(shooterConnection.getResponse());
        shooterConnection.write(PushGameEventCommand.newBuilder()
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
        waitUntilQueueNonEmpty(getShotConnection.getResponse());
        assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
        ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
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

        assertEquals(2, shooterConnection.getNetworkStats().getSentMessages(),
                "Only 2 messages must be sent by shooter: join + shoot");
        assertTrue(shooterConnection.getResponse().list().isEmpty(), "Shooter shouldn't receive any new messages. " +
                "Actual response "+ shooterConnection.getResponse().list());
        emptyQueue(shooterConnection.getResponse());
        shooterConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(shooterConnection.getResponse());
        var serverInfoResponse = shooterConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 kills player 2
     * @then player 2 is dead and gets disconnected. DEATH event is sent to all active players.
     */
    @Test
    public void testShootKill() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection killerConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        killerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection deadConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        deadConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(deadConnection.getResponse());
        waitUntilQueueNonEmpty(killerConnection.getResponse());
        emptyQueue(deadConnection.getResponse());

        ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
        emptyQueue(killerConnection.getResponse());

        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_DAMAGE);
        for (int i = 0; i < shotsToKill - 1; i++) {
            killerConnection.write(PushGameEventCommand.newBuilder()
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
            waitUntilQueueNonEmpty(deadConnection.getResponse());
            assertTrue(deadConnection.isConnected(), "Player is shot but still alive");
            assertTrue(killerConnection.isConnected(), "Killer must be connected");
            ServerResponse serverResponse = deadConnection.getResponse().poll().get();
            var shootingEvent = serverResponse.getGameEvents().getEvents(0);
            assertEquals(ServerResponse.GameEvent.GameEventType.GET_SHOT, shootingEvent.getEventType());
            assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
            assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
            assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
            assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
            assertEquals(100 - ServerConfig.DEFAULT_DAMAGE * (i + 1), shootingEvent.getAffectedPlayer().getHealth());
        }
        // this one kills player 2
        killerConnection.write(PushGameEventCommand.newBuilder()
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
        waitUntilQueueNonEmpty(deadConnection.getResponse());
        assertTrue(deadConnection.isDisconnected(), "Dead players should be disconnected");
        assertTrue(killerConnection.isConnected(), "Killer must be connected");

        ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
        var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.DEATH, deadShootingEvent.getEventType(),
                "Shot player must be dead");
        assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
        assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
        assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
        assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
        assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

        ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
        var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.DEATH, killerShootingEvent.getEventType(),
                "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
        assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

        killerConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(killerConnection.getResponse());
        var serverInfoResponse = killerConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "Must be 1 player only as 1 player got killed (it was 2)");
    }

    /**
     * @given a running server with 2 connected player: 1 active player and 2 dead
     * @when player 1 shoots player 2
     * @then nothing happens as dead players can't get shot
     */
    @Test
    public void testShootDeadPlayer() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection gameConnection1 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection1.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection gameConnection2 = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        gameConnection2.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(gameConnection1.getResponse());
        waitUntilQueueNonEmpty(gameConnection2.getResponse());
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
            waitUntilQueueNonEmpty(gameConnection2.getResponse());
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
        waitUntilQueueNonEmpty(gameConnection1.getResponse());
        var serverInfoResponse = gameConnection1.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "Must be 1 player only as 1 player got killed (it was 2)");
    }

    /**
     * @given a running server with 1 connected player
     * @when player shoots himself
     * @then player is disconnected
     */
    @Test
    public void testShootYourself() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection selfShootingConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);
        selfShootingConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(selfShootingConnection.getResponse());
        ServerResponse shooterPlayerSpawn = selfShootingConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(selfShootingConnection.getResponse());


        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;

        selfShootingConnection.write(PushGameEventCommand.newBuilder()
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

        waitUntilQueueNonEmpty(selfShootingConnection.getResponse());
        assertTrue(selfShootingConnection.isDisconnected());
        assertEquals(1, selfShootingConnection.getResponse().size(), "Should be one error response");

        ServerResponse errorResponse = selfShootingConnection.getResponse().poll().get();
        assertTrue(errorResponse.hasErrorEvent());
        ServerResponse.ErrorEvent errorEvent = errorResponse.getErrorEvent();
        assertEquals(GameErrorCode.CAN_NOT_SHOOT_YOURSELF.ordinal(), errorEvent.getErrorCode(),
                "You can't shoot yourself");

        GameConnection gameConnection = createGameConnection(ServerConfig.PASSWORD, "localhost", port);

        gameConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(gameConnection.getResponse());
        ServerResponse serverResponse = gameConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
        for (ServerResponse.GameInfo gameInfo : games) {
            assertEquals(0, gameInfo.getPlayersOnline(),
                    "Should be no connected players anywhere. The only player on server got disconnected");
        }
    }
}
