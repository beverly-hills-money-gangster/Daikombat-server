package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.SecondaryGameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.MergeConnectionCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
public class MergeConnectionTest extends AbstractGameServerTest {

  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a running game server and an established game connection
   * @when a player creates a secondary connection and merges it
   * @then merge is successful
   */
  @Test
  public void testMergeConnection() throws Exception {
    int gameIdToConnectTo = 0;
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());

    ServerResponse mySpawn = gameConnection.getResponse().poll().get();
    assertEquals(1, mySpawn.getGameEvents().getEventsCount(), "Should be only my spawn");
    assertEquals(1, mySpawn.getGameEvents().getPlayersOnline(), "Only me should be online");
    GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int myPlayerId = mySpawnGameEvent.getPlayer().getPlayerId();

    SecondaryGameConnection secondaryGameConnection = createSecondaryGameConnection(
        ServerConfig.PIN_CODE, "localhost", port);
    secondaryGameConnection.write(MergeConnectionCommand.newBuilder()
        .setPlayerId(myPlayerId).setGameId(gameIdToConnectTo)
        .build());

    Thread.sleep(1_000);
    assertTrue(secondaryGameConnection.isConnected(),
        "Secondary game connection should be kept connected");
    assertTrue(secondaryGameConnection.getErrors().list().isEmpty(),
        "No errors are expected");
    assertTrue(secondaryGameConnection.getResponse().list().isEmpty(),
        "No responses are expected");
  }

  /**
   * @given a running game server
   * @when a player creates a secondary connection and tries merging it with non-existing player id
   * @then merge fails
   */
  @Test
  public void testMergeConnectionNotExistingPlayerId() throws Exception {
    int gameIdToConnectTo = 0;
    SecondaryGameConnection secondaryGameConnection = createSecondaryGameConnection(
        ServerConfig.PIN_CODE, "localhost", port);
    secondaryGameConnection.write(MergeConnectionCommand.newBuilder()
        .setPlayerId(666).setGameId(gameIdToConnectTo)
        .build());

    Thread.sleep(1_000);
    assertFalse(secondaryGameConnection.isConnected(),
        "Secondary game connection should be disconnected as it failed");
  }

  /**
   * @given a running game server
   * @when a player creates a secondary connection and tries merging it with non-existing game id
   * @then merge fails
   */
  @Test
  public void testMergeConnectionNotExitingGameId() throws Exception {
    int gameIdToConnectTo = 666;
    SecondaryGameConnection secondaryGameConnection = createSecondaryGameConnection(
        ServerConfig.PIN_CODE, "localhost", port);
    secondaryGameConnection.write(MergeConnectionCommand.newBuilder()
        .setPlayerId(0).setGameId(gameIdToConnectTo)
        .build());

    Thread.sleep(1_000);
    assertFalse(secondaryGameConnection.isConnected(),
        "Secondary game connection should be disconnected as it failed");
  }

}
