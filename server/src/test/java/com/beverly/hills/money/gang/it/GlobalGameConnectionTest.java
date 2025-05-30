package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.GlobalGameConnection;
import com.beverly.hills.money.gang.network.SecondaryGameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.MergeConnectionCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.stats.GameNetworkStatsReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "250")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class GlobalGameConnectionTest extends AbstractGameServerTest {


  /**
   * @given 2 connections: player 1 and player 2. player 2 connection is load balanced
   * @when player 1 moves twice
   * @then player 2 sees player 1 move through the secondary connection
   */
  @Test
  public void testLoadBalancedGameConnection() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection movingPlayerConnection = createGameConnection( "localhost",
        port);
    movingPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(movingPlayerConnection.getResponse());
    ServerResponse mySpawn = movingPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection observerPlayerConnection = createGameConnection(
        "localhost", port);

    observerPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                PlayerClass.WARRIOR)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(observerPlayerConnection.getResponse(), 2);
    ServerResponse lbSpawn = observerPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent lbSpawnGameEvent = lbSpawn.getGameEvents().getEvents(0);
    int lbPlayerId = lbSpawnGameEvent.getPlayer().getPlayerId();

    SecondaryGameConnection secondaryGameConnection
        = createSecondaryGameConnection( "localhost", port);
    secondaryGameConnection.write(
        MergeConnectionCommand.newBuilder().setGameId(gameIdToConnectTo).setPlayerId(lbPlayerId)
            .build());
    Thread.sleep(1_000);
    assertTrue(secondaryGameConnection.isConnected(),
        "Secondary connection should be successfully connected");

    GlobalGameConnection globalGameConnection = new GlobalGameConnection(
        observerPlayerConnection,
        List.of(secondaryGameConnection));

    emptyQueue(observerPlayerConnection.getResponse());

    Thread.sleep(1_000);

    assertTrue(globalGameConnection.isAllConnected(),
        "All connections should be online");
    assertFalse(globalGameConnection.isAnyDisconnected(),
        "All connections should be online");

    float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 0.01f;
    float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 0.01f;
    emptyQueue(movingPlayerConnection.getResponse());

    movingPlayerConnection.write(PushGameEventCommand.newBuilder()
        .setMatchId(0).setGameId(gameIdToConnectTo)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerId1)
        .setPosition(Vector.newBuilder()
            .setY(newPositionY)
            .setX(newPositionX)
            .build())
        .setDirection(Vector.newBuilder()
            .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
            .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
            .build())
        .build());

    Thread.sleep(1_000);

    movingPlayerConnection.write(PushGameEventCommand.newBuilder()
        .setMatchId(0).setGameId(gameIdToConnectTo)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setEventType(PushGameEventCommand.GameEventType.MOVE)
        .setPlayerId(playerId1)
        .setPosition(Vector.newBuilder()
            .setY(newPositionY)
            .setX(newPositionX)
            .build())
        .setDirection(Vector.newBuilder()
            .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
            .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
            .build())
        .build());

    Thread.sleep(1_000);
    List<ServerResponse> lbResponses = globalGameConnection.pollResponses();
    assertEquals(2, lbResponses.size(),
        "2 responses are expected(player 1 moves)");

    ServerResponse moveServerResponse = lbResponses.get(0);
    assertEquals(2, moveServerResponse.getGameEvents().getPlayersOnline());
    assertTrue(moveServerResponse.hasGameEvents(), "Should be a game event");
    assertEquals(1, moveServerResponse.getGameEvents().getEventsCount(),
        "Only one game even is expected(player 1 move)");
    ServerResponse.GameEvent player1MoveEvent = moveServerResponse.getGameEvents().getEvents(0);

    assertTrue(player1MoveEvent.hasSequence());
    assertEquals(playerId1, player1MoveEvent.getPlayer().getPlayerId(), "Should be player 1 id");
    assertEquals(ServerResponse.GameEvent.GameEventType.MOVE, player1MoveEvent.getEventType());
    assertFalse(player1MoveEvent.hasLeaderBoard(), "We shouldn't receive leader boards on moves");
    assertEquals(mySpawnGameEvent.getPlayer().getDirection().getX(),
        player1MoveEvent.getPlayer().getDirection().getX(),
        0.00001, "Direction should not change");
    assertEquals(mySpawnGameEvent.getPlayer().getDirection().getY(),
        player1MoveEvent.getPlayer().getDirection().getY(),
        0.00001, "Direction should not change");

    assertEquals(newPositionX, player1MoveEvent.getPlayer().getPosition().getX(),
        0.00001);
    assertEquals(newPositionY, player1MoveEvent.getPlayer().getPosition().getY(),
        0.00001);

    Thread.sleep(1_000);
    assertEquals(0, globalGameConnection.pollResponses().size(),
        "No action so no response is expected");

    GameNetworkStatsReader secondaryNetworkStats = globalGameConnection.getSecondaryNetworkStats()
        .iterator()
        .next();
    assertEquals(1, secondaryNetworkStats.getReceivedMessages(),
        "Only one (MOVE) message is expected to be received by the secondary connection");
    assertEquals(1, secondaryNetworkStats.getSentMessages(),
        "Only one (MERGE) message is expected to be sent by the secondary connection");

    GameNetworkStatsReader primaryNetworkStats = globalGameConnection.getPrimaryNetworkStats();
    assertEquals(3, primaryNetworkStats.getReceivedMessages(),
        "Only 3 (my spawn, other player's spawn, and MOVE) messages are expected to be received by the primary connection");
  }

}
