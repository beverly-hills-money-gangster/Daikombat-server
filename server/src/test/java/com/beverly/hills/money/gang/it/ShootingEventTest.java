package com.beverly.hills.money.gang.it;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.RepeatedTest;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
public class ShootingEventTest extends AbstractGameServerTest {

    /**
     * @given a running server with 1 connected player
     * @when player 1 shoots and misses
     * @then nobody got shot
     */
    @RepeatedTest(8)
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
     * @given a running server with 1 connected player
     * @when player 1 punches and misses
     * @then nobody got punched
     */
    @RepeatedTest(8)
    public void testPunchMiss() throws IOException {
        int gameIdToConnectTo = 0;
        GameConnection puncherConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        puncherConnection.write(
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
        waitUntilQueueNonEmpty(puncherConnection.getResponse());
        waitUntilQueueNonEmpty(observerConnection.getResponse());
        emptyQueue(observerConnection.getResponse());
        ServerResponse mySpawn = puncherConnection.getResponse().poll().get();
        int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
        var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
        float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        emptyQueue(puncherConnection.getResponse());
        puncherConnection.write(PushGameEventCommand.newBuilder()
                .setPlayerId(playerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.PUNCH)
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
        assertEquals(1, observerConnection.getResponse().size(), "Only 1(punching) event is expected");
        ServerResponse serverResponse = observerConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
        assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One punching event is expected");
        assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(), "2 players are connected now");
        var punchingEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.PUNCH, punchingEvent.getEventType());
        assertFalse(punchingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
        assertEquals(playerId, punchingEvent.getPlayer().getPlayerId());

        assertEquals(mySpawnEvent.getPlayer().getDirection().getX(), punchingEvent.getPlayer().getDirection().getX(),
                "Direction shouldn't change");
        assertEquals(mySpawnEvent.getPlayer().getDirection().getY(), punchingEvent.getPlayer().getDirection().getY(),
                "Direction shouldn't change");
        assertEquals(newPositionX, punchingEvent.getPlayer().getPosition().getX());
        assertEquals(newPositionY, punchingEvent.getPlayer().getPosition().getY());

        assertEquals(100, punchingEvent.getPlayer().getHealth(), "Full health is nobody got punched(miss)");
        assertFalse(punchingEvent.hasAffectedPlayer(), "Nobody is affected. Missed the attack");
        assertEquals(2, puncherConnection.getNetworkStats().getSentMessages(),
                "Only 2 messages must be sent by puncher: join + punch");
        assertTrue(puncherConnection.getResponse().list().isEmpty(), "Puncher shouldn't receive any new messages");
    }


    /**
     * @given a running server with 2 connected player
     * @when player 1 shoots player 2
     * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE and the event is sent to all players
     */
    @RepeatedTest(8)
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
        assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE, shootingEvent.getAffectedPlayer().getHealth());
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
     * @when player 1 punches player 2
     * @then player 2 health is reduced by ServerConfig.DEFAULT_PUNCH_DAMAGE and the event is sent to all players
     */
    @RepeatedTest(8)
    public void testPunchHit() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection punchingConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        punchingConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection getPunchedConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        getPunchedConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(punchingConnection.getResponse());
        ServerResponse puncherPlayerSpawn = punchingConnection.getResponse().poll().get();
        int shooterPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
        ServerResponse punchedPlayerSpawn = punchingConnection.getResponse().poll().get();
        int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(getPunchedConnection.getResponse());


        var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = puncherSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = puncherSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        emptyQueue(punchingConnection.getResponse());
        punchingConnection.write(PushGameEventCommand.newBuilder()
                .setPlayerId(shooterPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.PUNCH)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                                .setY(puncherSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .setAffectedPlayerId(punchedPlayerId)
                .build());
        waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
        assertEquals(1, getPunchedConnection.getResponse().size(), "Only 1(punching) event is expected");
        ServerResponse serverResponse = getPunchedConnection.getResponse().poll().get();
        assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
        assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One punch event is expected");
        assertEquals(2, serverResponse.getGameEvents().getPlayersOnline(), "2 players are connected now");
        var punchingEvent = serverResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.GET_PUNCHED, punchingEvent.getEventType());
        assertFalse(punchingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
        assertEquals(shooterPlayerId, punchingEvent.getPlayer().getPlayerId());

        assertEquals(puncherSpawnEvent.getPlayer().getDirection().getX(), punchingEvent.getPlayer().getDirection().getX(),
                "Direction shouldn't change");
        assertEquals(puncherSpawnEvent.getPlayer().getDirection().getY(), punchingEvent.getPlayer().getDirection().getY(),
                "Direction shouldn't change");
        assertEquals(newPositionX, punchingEvent.getPlayer().getPosition().getX());
        assertEquals(newPositionY, punchingEvent.getPlayer().getPosition().getY());

        assertEquals(100, punchingEvent.getPlayer().getHealth(), "Puncher player health is full");
        assertTrue(punchingEvent.hasAffectedPlayer(), "One player must be punched");
        assertEquals(punchedPlayerId, punchingEvent.getAffectedPlayer().getPlayerId());
        assertEquals(100 - ServerConfig.DEFAULT_PUNCH_DAMAGE, punchingEvent.getAffectedPlayer().getHealth());
        assertEquals(punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
                punchingEvent.getAffectedPlayer().getPosition().getX(),
                "Punched player hasn't moved so position has to stay the same");
        assertEquals(punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
                punchingEvent.getAffectedPlayer().getPosition().getY(),
                "Punched player hasn't moved so position has to stay the same");

        assertEquals(2, punchingConnection.getNetworkStats().getSentMessages(),
                "Only 2 messages must be sent by puncher: join + punch");
        assertTrue(punchingConnection.getResponse().list().isEmpty(), "Puncher shouldn't receive any new messages. " +
                "Actual response " + punchingConnection.getResponse().list());
        emptyQueue(punchingConnection.getResponse());
        punchingConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(punchingConnection.getResponse());
        var serverInfoResponse = punchingConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 shoots player 2 too far way
     * @then player 1 event is not published to player 2
     */
    @RepeatedTest(8)
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
        ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        waitUntilQueueNonEmpty(getShotConnection.getResponse());
        ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
        int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
        var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);

        Thread.sleep(500);
        emptyQueue(getShotConnection.getResponse());
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
        assertEquals(0, getShotConnection.getResponse().size(), "No response is expected. " +
                "Actual response: " + getShotConnection.getResponse().list());

        assertTrue(getShotConnection.isConnected());
        assertTrue(shooterConnection.isConnected());
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 punches player 2 too far way
     * @then player 1 event is not published to player 2
     */
    @RepeatedTest(8)
    public void testShootPunchTooFar() throws Exception {
        int gameIdToConnectTo = 0;
        GameConnection puncherConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        puncherConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my player name")
                        .setGameId(gameIdToConnectTo).build());
        GameConnection getPunchedConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        getPunchedConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());

        waitUntilQueueNonEmpty(puncherConnection.getResponse());
        ServerResponse puncherPlayerSpawn = puncherConnection.getResponse().poll().get();
        int puncherPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
        ServerResponse punchedPlayerSpawn = puncherConnection.getResponse().poll().get();
        int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
        var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);


        emptyQueue(getPunchedConnection.getResponse());
        emptyQueue(puncherConnection.getResponse());

        puncherConnection.write(PushGameEventCommand.newBuilder()
                .setPlayerId(puncherPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.PUNCH)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                                .setY(puncherSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                // too far
                                .setX(-1000f)
                                .setY(+1000f)
                                .build())
                .setAffectedPlayerId(punchedPlayerId)
                .build());
        Thread.sleep(1_000);
        assertEquals(0, getPunchedConnection.getResponse().size(), "No response is expected. " +
                "Actual response: " + getPunchedConnection.getResponse().list());

        assertTrue(getPunchedConnection.isConnected());
        assertTrue(puncherConnection.isConnected());
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 kills player 2
     * @then player 2 is dead and gets disconnected. KILL event is sent to all active players.
     */
    @RepeatedTest(8)
    public void testShootKill() throws Exception {
        int gameIdToConnectTo = 0;
        String shooterPlayerName = "killer";
        GameConnection killerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        killerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName(shooterPlayerName)
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
        int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);
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
            assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE * (i + 1), shootingEvent.getAffectedPlayer().getHealth());
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
        assertTrue(deadConnection.isConnected(), "Dead players should be disconnected");
        assertTrue(killerConnection.isConnected(), "Killer must be connected");

        ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
        var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.KILL_SHOOTING, deadShootingEvent.getEventType(),
                "Shot player must be dead");
        assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");


        assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
        assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
        assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
        assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
        assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

        ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
        var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
        assertEquals(1, killerPlayerServerResponse.getGameEvents().getPlayersOnline(),
                "Only one player should be online. The other one must be dead.");
        assertEquals(ServerResponse.GameEvent.GameEventType.KILL_SHOOTING, killerShootingEvent.getEventType(),
                "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
        assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

        killerConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(killerConnection.getResponse());
        var serverInfoResponse = killerConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "Must be 1 player only as 1 player got killed (it was 2)");

        String observerPlayerName = "observer";
        GameConnection observerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName(observerPlayerName)
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(observerConnection.getResponse());

        var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents().getEvents(0);
        int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();
        assertTrue(observerPlayerSpawn.hasLeaderBoard(), "Newly connected players must have leader board");
        assertEquals(2, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
                "There must be 2 items in the board at this moment: killer + observer");

        assertEquals(shooterPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
        assertEquals(shooterPlayerName, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerName());
        assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());

        assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
        assertEquals(observerPlayerName, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerName());
        assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    }

    /**
     * @given a running server with 2 connected player
     * @when player 1 kills player 2 by punching
     * @then player 2 is dead and gets disconnected. KILL event is sent to all active players.
     */
    @RepeatedTest(8)
    public void testPunchKill() throws Exception {
        int gameIdToConnectTo = 0;
        String puncherPlayerName = "killer";
        GameConnection puncherConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        puncherConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName(puncherPlayerName)
                        .setGameId(gameIdToConnectTo).build());

        GameConnection deadConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        deadConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName("my other player name")
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(puncherConnection.getResponse());
        ServerResponse puncherPlayerSpawn = puncherConnection.getResponse().poll().get();
        int puncherPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        waitUntilQueueNonEmpty(deadConnection.getResponse());
        ServerResponse punchedPlayerSpawn = puncherConnection.getResponse().poll().get();
        int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

        emptyQueue(deadConnection.getResponse());
        emptyQueue(puncherConnection.getResponse());

        var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);
        float newPositionX = puncherSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
        float newPositionY = puncherSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
        int punchesToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_PUNCH_DAMAGE);
        for (int i = 0; i < punchesToKill - 1; i++) {
            puncherConnection.write(PushGameEventCommand.newBuilder()
                    .setPlayerId(puncherPlayerId)
                    .setGameId(gameIdToConnectTo)
                    .setEventType(PushGameEventCommand.GameEventType.PUNCH)
                    .setDirection(
                            PushGameEventCommand.Vector.newBuilder()
                                    .setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                                    .setY(puncherSpawnEvent.getPlayer().getDirection().getY())
                                    .build())
                    .setPosition(
                            PushGameEventCommand.Vector.newBuilder()
                                    .setX(newPositionX)
                                    .setY(newPositionY)
                                    .build())
                    .setAffectedPlayerId(punchedPlayerId)
                    .build());
            waitUntilQueueNonEmpty(deadConnection.getResponse());
            assertTrue(deadConnection.isConnected(), "Player is punched but still alive");
            assertTrue(puncherConnection.isConnected(), "Killer must be connected");
            ServerResponse serverResponse = deadConnection.getResponse().poll().get();
            var punchingEvent = serverResponse.getGameEvents().getEvents(0);
            assertEquals(ServerResponse.GameEvent.GameEventType.GET_PUNCHED, punchingEvent.getEventType());
            assertEquals(puncherPlayerId, punchingEvent.getPlayer().getPlayerId());
            assertEquals(100, punchingEvent.getPlayer().getHealth(), "Puncher player health is full");
            assertTrue(punchingEvent.hasAffectedPlayer(), "One player must be punched");
            assertEquals(punchedPlayerId, punchingEvent.getAffectedPlayer().getPlayerId());
            assertEquals(100 - ServerConfig.DEFAULT_PUNCH_DAMAGE * (i + 1), punchingEvent.getAffectedPlayer().getHealth());
        }
        // this one kills player 2
        puncherConnection.write(PushGameEventCommand.newBuilder()
                .setPlayerId(puncherPlayerId)
                .setGameId(gameIdToConnectTo)
                .setEventType(PushGameEventCommand.GameEventType.PUNCH)
                .setDirection(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                                .setY(puncherSpawnEvent.getPlayer().getDirection().getY())
                                .build())
                .setPosition(
                        PushGameEventCommand.Vector.newBuilder()
                                .setX(newPositionX)
                                .setY(newPositionY)
                                .build())
                .setAffectedPlayerId(punchedPlayerId)
                .build());
        waitUntilQueueNonEmpty(deadConnection.getResponse());
        assertFalse(deadConnection.isDisconnected(), "Dead players should be kept connected for graceful shutdown");
        assertTrue(puncherConnection.isConnected(), "Killer must be connected");

        ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
        var deadPunchingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
        assertEquals(1, deadPlayerServerResponse.getGameEvents().getPlayersOnline(),
                "Only one player should be online. The other one must be dead.");
        assertEquals(ServerResponse.GameEvent.GameEventType.KILL_PUNCHING, deadPunchingEvent.getEventType(),
                "Punched player must be dead");
        assertFalse(deadPunchingEvent.hasLeaderBoard(), "Leader board are published only on spawns");


        assertEquals(puncherPlayerId, deadPunchingEvent.getPlayer().getPlayerId());
        assertEquals(100, deadPunchingEvent.getPlayer().getHealth(), "Shooter player health is full");
        assertTrue(deadPunchingEvent.hasAffectedPlayer(), "One player must be punched");
        assertEquals(punchedPlayerId, deadPunchingEvent.getAffectedPlayer().getPlayerId());
        assertEquals(0, deadPunchingEvent.getAffectedPlayer().getHealth());

        ServerResponse killerPlayerServerResponse = puncherConnection.getResponse().poll().get();
        var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
        assertEquals(ServerResponse.GameEvent.GameEventType.KILL_PUNCHING, killerShootingEvent.getEventType(),
                "Punched player must be dead. Actual response is " + killerPlayerServerResponse);
        assertEquals(puncherPlayerId, killerShootingEvent.getPlayer().getPlayerId());

        puncherConnection.write(GetServerInfoCommand.newBuilder().build());
        waitUntilQueueNonEmpty(puncherConnection.getResponse());
        var serverInfoResponse = puncherConnection.getResponse().poll().get();
        List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
        ServerResponse.GameInfo myGame = games.stream().filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst()
                .orElseThrow((Supplier<Exception>) () -> new IllegalStateException("Can't find the game we connected to"));
        assertEquals(1, myGame.getPlayersOnline(), "Must be 1 player only as 1 player got killed (it was 2)");

        String observerPlayerName = "observer";
        GameConnection observerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
        observerConnection.write(
                JoinGameCommand.newBuilder()
                        .setVersion(ServerConfig.VERSION)
                        .setPlayerName(observerPlayerName)
                        .setGameId(gameIdToConnectTo).build());
        waitUntilQueueNonEmpty(observerConnection.getResponse());

        var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents().getEvents(0);
        int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();
        assertTrue(observerPlayerSpawn.hasLeaderBoard(), "Newly connected players must have leader board");
        assertEquals(2, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
                "There must be 2 items in the board at this moment: killer + observer");

        assertEquals(puncherPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
        assertEquals(puncherPlayerName, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerName());
        assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());

        assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
        assertEquals(observerPlayerName, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerName());
        assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    }


    /**
     * @given a running server with 2 connected player: 1 active player and 2 dead
     * @when player 1 shoots player 2
     * @then nothing happens as dead players can't get shot
     */
    @RepeatedTest(8)
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
        int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);
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
                assertEquals(ServerResponse.GameEvent.GameEventType.KILL_SHOOTING, shootingEvent.getEventType());
            } else {
                assertEquals(ServerResponse.GameEvent.GameEventType.GET_SHOT, shootingEvent.getEventType());
            }
            assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
            assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
            assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
            assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
            assertEquals(100 - ServerConfig.DEFAULT_SHOTGUN_DAMAGE * (i + 1), shootingEvent.getAffectedPlayer().getHealth());
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
    @RepeatedTest(8)
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
        assertEquals(GameErrorCode.CAN_NOT_ATTACK_YOURSELF.ordinal(), errorEvent.getErrorCode(),
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
