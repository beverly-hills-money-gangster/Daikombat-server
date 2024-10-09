package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "9999")
@SetEnvironmentVariable(key = "GAME_SERVER_PLAYER_SPEED_CHECK_FREQUENCY_MLS", value = "1000")
public class MoveAntiCheatTest extends AbstractGameServerTest {


  /**
   * @given a running server with 2 connected players
   * @when player 1 move too fast, player 2 observes
   * @then player 1 is disconnected, player 2 sees player exit
   */
  @Test
  public void testMoveTooFast() throws Exception {
    int gameIdToConnectTo = 2;
    GameConnection cheatingPlayerConnection = createGameConnection(
        "localhost", port);
    cheatingPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN).setPlayerClass(
                PlayerClass.COMMONER)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(cheatingPlayerConnection.getResponse());
    ServerResponse mySpawn = cheatingPlayerConnection.getResponse().poll().get();
    ServerResponse.GameEvent mySpawnGameEvent = mySpawn.getGameEvents().getEvents(0);
    int playerId1 = mySpawnGameEvent.getPlayer().getPlayerId();

    GameConnection observerPlayerConnection = createGameConnection(
        "localhost", port);
    observerPlayerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN).setPlayerClass(PlayerClass.COMMONER)
            .setPlayerName("new player")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerPlayerConnection.getResponse());
    waitUntilQueueNonEmpty(cheatingPlayerConnection.getResponse());

    emptyQueue(observerPlayerConnection.getResponse());
    emptyQueue(cheatingPlayerConnection.getResponse());

    // moving too fast
    for (int i = 0; i < 3; i++) {
      float newPositionY = mySpawnGameEvent.getPlayer().getPosition().getY() + 100f * (i + 1);
      float newPositionX = mySpawnGameEvent.getPlayer().getPosition().getX() + 100f * (i + 1);
      cheatingPlayerConnection.write(PushGameEventCommand.newBuilder()
          .setGameId(gameIdToConnectTo)
          .setSequence(sequenceGenerator.getNext())
          .setPingMls(PING_MLS)
          .setEventType(PushGameEventCommand.GameEventType.MOVE)
          .setPlayerId(playerId1)
          .setPosition(PushGameEventCommand.Vector.newBuilder()
              .setY(newPositionY)
              .setX(newPositionX)
              .build())
          .setDirection(PushGameEventCommand.Vector.newBuilder()
              .setY(mySpawnGameEvent.getPlayer().getDirection().getY())
              .setX(mySpawnGameEvent.getPlayer().getDirection().getX())
              .build())
          .build());
    }

    Thread.sleep(ServerConfig.PLAYER_SPEED_CHECK_FREQUENCY_MLS * 3L);
    assertTrue(cheatingPlayerConnection.isDisconnected(), "Cheating player should be disconnected");
    assertTrue(observerPlayerConnection.isConnected());

    waitUntilGetResponses(observerPlayerConnection.getResponse(), 1);
    assertEquals(1, observerPlayerConnection.getResponse().size(),
        "Only one exit event is expected. Actual response is "
            + observerPlayerConnection.getResponse().list());
    ServerResponse exitServerResponse = observerPlayerConnection.getResponse().poll().get();
    assertEquals(1, exitServerResponse.getGameEvents().getPlayersOnline(),
        "Only 1 player is expected to be online now. Cheating player should exit.");
    assertTrue(exitServerResponse.hasGameEvents(), "Should be a game event");
    assertEquals(1, exitServerResponse.getGameEvents().getEventsCount(),
        "Only one game even is expected(player 1 exit)");
    ServerResponse.GameEvent playerExitEvent = exitServerResponse.getGameEvents().getEvents(0);

    assertEquals(playerId1, playerExitEvent.getPlayer().getPlayerId(), "Should be player 1 id");
    assertEquals(ServerResponse.GameEvent.GameEventType.EXIT, playerExitEvent.getEventType());
  }


}
