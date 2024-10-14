package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.Vector;
import java.io.IOException;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "1000")
public class IdleServerTest extends AbstractGameServerTest {

  /**
   * @given a running game server and 1 connected player
   * @when server sends no update for long time
   * @then the player disconnects from the server
   */
  @Test
  public void testClientIsMovingButServerIsIdle() throws IOException, InterruptedException {
    int gameToConnectTo = 1;
    GameConnection gameConnection = createGameConnection( "localhost", port);
    gameConnection.shutdownPingScheduler(); // shutting down to mimic server inactivity
    gameConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                PlayerClass.COMMONER)
            .setPlayerName("my player name")
            .setGameId(gameToConnectTo).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    var mySpawn = gameConnection.getResponse().poll().get().getGameEvents().getEvents(0);
    int playerId = mySpawn.getPlayer().getPlayerId();

    gameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    var myGame = serverResponse.getServerInfo().getGamesList().stream().filter(gameInfo
            -> gameInfo.getGameId() == gameToConnectTo).findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Can't find game by id. Response is:" + serverResponse));

    assertEquals(1, myGame.getPlayersOnline(), "Only the current player should be connected");
    emptyQueue(gameConnection.getResponse());

    float newPositionY = mySpawn.getPlayer().getPosition().getY();
    float newPositionX = mySpawn.getPlayer().getPosition().getX();
    // move
    for (int i = 0; i < 60; i++) {
      newPositionY += 0.1f;
      newPositionX += 0.1f;
      gameConnection.write(PushGameEventCommand.newBuilder()
          .setPlayerId(playerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setGameId(gameToConnectTo)
          .setEventType(PushGameEventCommand.GameEventType.MOVE)
          .setDirection(Vector.newBuilder().setX(0).setY(1).build())
          .setPosition(Vector.newBuilder()
              .setX(newPositionX).setY(newPositionY).build())
          .build());
      Thread.sleep(200);
    }

    assertEquals(0, gameConnection.getResponse().size(),
        "It's expected that server didn't send anything in a while. Response is :"
            + gameConnection.getResponse().list());
    assertEquals(1, gameConnection.getErrors().size());
    Throwable error = gameConnection.getErrors().poll().get();
    assertEquals(IOException.class, error.getClass(),
        "Inactive server should be treated as an IO exception");
    assertEquals("Server is inactive for too long", error.getMessage());
    assertTrue(gameConnection.isDisconnected(),
        "Client should disconnect because the server is idle");
  }
}
