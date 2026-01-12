package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.ServerResponse;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class GameConnectionTest extends AbstractGameServerTest {

  /**
   * @given a running game server with 2 connected players
   * @when player 1 disconnects from server
   * @then player 1 gets disconnected and player 2 has the event DISCONNECT for player 1
   */
  @RepeatedTest(4)
  public void testExit() throws IOException, InterruptedException {
    int gameToConnectTo = 0;
    var gameConnection1 = createGameConnection("localhost", port);
    gameConnection1.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection1.getResponse());
    var mySpawn1 = gameConnection1.getResponse().poll().get();
    var mySpawnGameEvent1 = mySpawn1.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent1.getPlayer().getPlayerId();

    var gameConnection2 = createGameConnection("localhost", port);
    gameConnection2.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my other player name")
            .setGameId(gameToConnectTo).build());
    waitUntilGetResponses(gameConnection2.getResponse(), 2);
    waitUntilQueueNonEmpty(gameConnection1.getResponse());

    emptyQueue(gameConnection1.getResponse());
    emptyQueue(gameConnection2.getResponse());
    gameConnection1.disconnect();
    Thread.sleep(250);
    assertFalse(gameConnection1.isConnected(), "Player 1 should be disconnected now");
    assertTrue(gameConnection2.isConnected(), "Player 2 should be connected");

    gameConnection1.write(GetServerInfoCommand.newBuilder()
        .setPlayerClass(PlayerClass.WARRIOR).build());
    Thread.sleep(250);
    assertEquals(0, gameConnection1.getResponse().size(),
        "Should be no response because the connection is closed. Actual response: "
            + gameConnection1.getResponse().list());
    assertEquals(1, gameConnection1.getWarning().size(),
        "Should be one warning because the connection is closed. Actual warnings: "
            + gameConnection1.getWarning().list());
    Throwable error = gameConnection1.getWarning().poll().get();
    assertEquals(IOException.class, error.getClass());
    assertEquals("Can't write using closed connection", error.getMessage());

    assertEquals(1,
        gameConnection2.getResponse().size(),
        "We need to get 1 response(EXIT). Actual response: " + gameConnection2.getResponse()
            .list());
    ServerResponse serverResponse = gameConnection2.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents());
    assertEquals(1, serverResponse.getGameEvents().getEventsCount());
    var disconnectEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(playerId1, disconnectEvent.getPlayer().getPlayerId());
    assertEquals(ServerResponse.GameEvent.GameEventType.EXIT, disconnectEvent.getEventType());
    assertEquals(1, serverResponse.getGameEvents().getPlayersOnline(),
        "1 player left because the other disconnected himself");
  }

  /**
   * @given a running game server with 1 connected player
   * @when player 1 disconnects from server twice
   * @then player 1 gets disconnected. 2nd disconnect attempt does not cause any issues
   */
  @Test
  public void testDisconnectTwice() throws IOException, InterruptedException {
    var gameConnection = createGameConnection("localhost", port);
    assertTrue(gameConnection.isConnected(), "Should be connected by default");
    assertFalse(gameConnection.isDisconnected());
    gameConnection.disconnect();
    gameConnection.disconnect(); // call twice
    Thread.sleep(250);
    assertTrue(gameConnection.isDisconnected(), "Should be disconnected after disconnecting");
    assertFalse(gameConnection.isConnected());
    gameConnection.write(GetServerInfoCommand.newBuilder()
        .setPlayerClass(PlayerClass.WARRIOR).build());
    Thread.sleep(250);
    assertEquals(0, gameConnection.getResponse().size(),
        "Should be no response because the connection is closed. Actual "
            + gameConnection.getResponse());
    assertEquals(1, gameConnection.getWarning().size(),
        "Should be one warning because the connection is closed");
    Throwable error = gameConnection.getWarning().poll().get();
    assertEquals(IOException.class, error.getClass());
    assertEquals("Can't write using closed connection", error.getMessage());
  }

  /**
   * @given a running game server
   * @when player 1 connects to a non-existing host
   * @then an exception is thrown
   */
  @Test
  public void testGetServerInfoNotExistingServer() throws IOException, InterruptedException {
    var connection = createGameConnection("666.666.666.666", port);
    assertInstanceOf(UnknownHostException.class, connection.getErrors().poll().get());
  }

  /**
   * @given a running game server
   * @when player 1 connects to correct host but incorrect port
   * @then an exception is thrown
   */
  @Test
  public void testGetServerInfoWrongPort() throws IOException, InterruptedException {
    var connection = createGameConnection("localhost", 666);
    assertInstanceOf(ConnectException.class, connection.getErrors().poll().get());
  }
}
