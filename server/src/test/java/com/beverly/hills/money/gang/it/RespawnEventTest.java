package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import org.junit.jupiter.api.RepeatedTest;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
public class RespawnEventTest extends AbstractGameServerTest {

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 and then player 2 respawns
   * @then player 1 sees player 2 respawning
   */
  @RepeatedTest(8)
  public void testRespawn() throws Exception {
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    GameConnection killerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
        port);
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
    LOG.info("Shot player spawn {}", shotPlayerSpawn);
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotsToKill = (int) Math.ceil(100D / ServerConfig.DEFAULT_SHOTGUN_DAMAGE);
    for (int i = 0; i < shotsToKill; i++) {
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
    }

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    Thread.sleep(500);

    emptyQueue(killerConnection.getResponse());
    emptyQueue(deadConnection.getResponse());

    deadConnection.write(RespawnCommand.newBuilder()
        .setPlayerId(shotPlayerId).setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());
    ServerResponse respawnResponse = deadConnection.getResponse().poll()
        .orElseThrow(() -> new IllegalStateException("There should be a spawn event"));
    assertEquals(1, respawnResponse.getGameEvents().getEventsCount());

    var respawnEvent = respawnResponse.getGameEvents().getEvents(0);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, respawnEvent.getEventType());
    assertEquals(shotPlayerId, respawnEvent.getPlayer().getPlayerId());

    waitUntilQueueNonEmpty(killerConnection.getResponse());
    assertTrue(killerConnection.getResponse().list().stream()
            .anyMatch(serverResponse -> serverResponse.hasGameEvents()
                && serverResponse.getGameEvents().getEvents(0).getEventType()
                == ServerResponse.GameEvent.GameEventType.SPAWN
                && serverResponse.getGameEvents().getEvents(0).getPlayer().getPlayerId()
                == shotPlayerId),
        "Killer must see the respawn. Actual response: " + killerConnection.getResponse());

  }

}
