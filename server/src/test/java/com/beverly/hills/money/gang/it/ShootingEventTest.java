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

@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_PING_FREQUENCY_MLS", value = "99999")
public class ShootingEventTest extends AbstractGameServerTest {

    /**
     * @given a running server with 1 connected player
     * @when player 1 shoots and misses
     * @then nobody got shot
     */
    @Test
    public void testShootMiss() throws IOException {
        int gameIdToConnectTo = 0;
        GameConnection shooterConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        shooterConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection observerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
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
        assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
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
        GameConnection shooterConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        shooterConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection getShotConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        getShotConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(shooterConnection.getResponse());
        ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        waitUntilQueueNonEmpty(getShotConnection.getResponse());
        ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(getShotConnection.getResponse());


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
        assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
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
                "Actual response " + shooterConnection.getResponse().list());
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
     * @when player 1 shoots player 2 too far way
     * @then player 1 is disconnected for cheating, player 2 receives EXIT event from player 1
     */
    @Test
    public void testShootHitTooFar() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection shooterConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        shooterConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection getShotConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
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
                                // too far
                                .setX(-1000f)
                                .setY(+1000f)
                                .build())
                .setAffectedPlayerId(shotPlayerId)
                .build());
        Thread.sleep(1_000);
        waitUntilQueueNonEmpty(getShotConnection.getResponse());
        assertEquals(1, getShotConnection.getResponse().size(), "Only 1(exit) event is expected. " +
                "Actual response: " + getShotConnection.getResponse().list());

        ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
        assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One exit event is expected");
        assertEquals(1, serverResponse.getGameEvents().getPlayersOnline(), "1 players is connected now");


        var exitEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.EXIT, exitEvent.getEventType());
        assertEquals(shooterPlayerId, exitEvent.getPlayer().getPlayerId());

        assertTrue(getShotConnection.isConnected());
        assertTrue(shooterConnection.isDisconnected(), "Shooter must be disconnected for cheating");
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 kills player 2
     * @then player 2 is dead and gets disconnected. DEATH event is sent to all active players.
     */
    @Test
    public void testShootKill() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection killerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        killerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());

        GameConnection deadConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        deadConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(killerConnection.getResponse());
        ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        waitUntilQueueNonEmpty(deadConnection.getResponse());
        ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(deadConnection.getResponse());
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
        assertTrue(deadShootingEvent.hasLeaderBoard(), "We must have leader board as somebody got killed");
        assertEquals(1, deadShootingEvent.getLeaderBoard().getItemsCount(),
                "Only alive players must be in the leader board");
        assertEquals(1, deadShootingEvent.getLeaderBoard().getItems(0).getKills(),
                "Killer player killed one");
        assertEquals(shooterPlayerId, deadShootingEvent.getLeaderBoard().getItems(0).getPlayerId(),
                "Killer player killed one");


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


        GameConnection observerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("observer player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(observerConnection.getResponse());

        var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents().getEvents(0);
        int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();
        assertTrue(observerPlayerSpawn.hasLeaderBoard(), "Newly connected players must have leader board");
        assertEquals(2, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
                "There must be 2 items in the board at this moment: killer + observer");

        assertEquals(shooterPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
        assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());

        assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
        assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    }

    /**
     * @given a running server with 2 connected player: 1 active player and 2 dead
     * @when player 1 shoots player 2
     * @then nothing happens as dead players can't get shot
     */
    @Test
    public void testShootDeadPlayer() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection shooterConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        shooterConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection deadPlayerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        deadPlayerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(shooterConnection.getResponse());
        waitUntilQueueNonEmpty(deadPlayerConnection.getResponse());
        emptyQueue(deadPlayerConnection.getResponse());

        ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() + 0.1f;
        int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_DAMAGE);
        for (int i = 0; i < shotsToKill; i++) {
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
            waitUntilQueueNonEmpty(deadPlayerConnection.getResponse());
            ServerResponse serverResponse = deadPlayerConnection.getResponse().poll().get();
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
        emptyQueue(shooterConnection.getResponse());
        // shoot dead player
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
        Thread.sleep(250);
        assertEquals(0, shooterConnection.getResponse().size(),
                "Should be no response as you can't shoot a dead player");
        assertEquals(0, shooterConnection.getWarning().size(),
                "Should be no warnings");
        assertEquals(0, shooterConnection.getErrors().size(),
                "Should be no errors as this situation might happen in a fast paced game");

        emptyQueue(shooterConnection.getResponse());
        shooterConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(shooterConnection.getResponse());
        var serverInfoResponse = shooterConnection.getResponse().poll().get();
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
        GameConnection selfShootingConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
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

        GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);

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
