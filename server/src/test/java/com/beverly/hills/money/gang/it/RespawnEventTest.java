package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.RespawnCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class RespawnEventTest extends AbstractGameServerTest {


  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 and then player 2 respawns
   * @then player 1 sees player 2 respawning
   */
  @Test
  public void testRespawn() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    String shooterPlayerName = "killer";
    GameConnection killerConnection = createGameConnection("localhost",
        port);
    killerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.PINK)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.PURPLE)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());

    waitUntilGetResponses(killerConnection.getResponse(), 2);
    waitUntilGetResponses(deadConnection.getResponse(), 2);

    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    LOG.info("Shot player spawn {}", shotPlayerSpawn);
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotsToKill = (int) Math.ceil(100D / gameConfig.getDefaultShotgunDamage());
    for (int i = 0; i < shotsToKill; i++) {
      killerConnection.write(PushGameEventCommand.newBuilder()
          .setPlayerId(shooterPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setMatchId(0).setGameId(gameIdToConnectTo)
          .setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.SHOTGUN)
          .setDirection(
              Vector.newBuilder()
                  .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                  .build())
          .setPosition(
              Vector.newBuilder()
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
        .setPlayerId(shotPlayerId).setMatchId(0).setGameId(gameIdToConnectTo)
        .build());
    waitUntilGetResponses(deadConnection.getResponse(), 2);
    ServerResponse respawnResponse = deadConnection.getResponse().poll()
        .orElseThrow(() -> new IllegalStateException("There should be a spawn event"));
    assertEquals(1, respawnResponse.getGameEvents().getEventsCount());

    var otherPlayersSpawnResponse = deadConnection.getResponse().poll()
        .orElseThrow(() -> new IllegalStateException("There should be other players' spawn event"));
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN,
        otherPlayersSpawnResponse.getGameEvents().getEvents(0).getEventType());
    assertEquals(shooterPlayerId,
        otherPlayersSpawnResponse.getGameEvents().getEvents(0).getPlayer().getPlayerId());
    assertEquals(PlayerSkinColor.PINK,
        otherPlayersSpawnResponse.getGameEvents().getEvents(0).getPlayer().getSkinColor());

    var respawnEvent = respawnResponse.getGameEvents().getEvents(0);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, respawnEvent.getEventType());
    assertEquals(shotPlayerId, respawnEvent.getPlayer().getPlayerId());

    waitUntilQueueNonEmpty(killerConnection.getResponse());
    assertTrue(killerConnection.getResponse().list().stream()
            .anyMatch(serverResponse -> serverResponse.hasGameEvents()
                && serverResponse.getGameEvents().getEvents(0).getEventType()
                == GameEvent.GameEventType.RESPAWN
                && serverResponse.getGameEvents().getEvents(0).getPlayer().getSkinColor()
                == PlayerSkinColor.PURPLE
                && serverResponse.getGameEvents().getEvents(0).getPlayer().getPlayerId()
                == shotPlayerId),
        "Killer must see the respawn. Actual response: " + killerConnection.getResponse());

  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 and then player 2 respawns with a wrong match id
   * @then player 1 doesn't see player 2 respawning
   */
  @Test
  public void testRespawnWrongMatchId() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    String shooterPlayerName = "killer";
    GameConnection killerConnection = createGameConnection("localhost",
        port);
    killerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.PINK)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    GameConnection deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.PURPLE)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());

    waitUntilGetResponses(killerConnection.getResponse(), 2);
    waitUntilGetResponses(deadConnection.getResponse(), 2);

    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    LOG.info("Shot player spawn {}", shotPlayerSpawn);
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotsToKill = (int) Math.ceil(100D / gameConfig.getDefaultShotgunDamage());
    for (int i = 0; i < shotsToKill; i++) {
      killerConnection.write(PushGameEventCommand.newBuilder()
          .setPlayerId(shooterPlayerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setMatchId(0).setGameId(gameIdToConnectTo)
          .setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.SHOTGUN)
          .setDirection(
              Vector.newBuilder()
                  .setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY())
                  .build())
          .setPosition(
              Vector.newBuilder()
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
        .setPlayerId(shotPlayerId)
        .setMatchId(123) // wrong match id
        .setGameId(gameIdToConnectTo).build());
    // we get nothing
    Thread.sleep(5_000);
    assertTrue(deadConnection.getResponse().list().isEmpty(),
        "We should have nothing here because the respawn match id was wrong. Actual response "
            + deadConnection.getResponse().list());
  }

}
