package com.beverly.hills.money.gang.it;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.getWeaponType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SetEnvironmentVariable(key = "GAME_SERVER_DEFAULT_RAILGUN_DAMAGE", value = "100")
@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "5000")
@SetEnvironmentVariable(key = "CLIENT_UDP_GLITCHY_INBOUND_DROP_MESSAGE_PROBABILITY", value = "0.2")
@SetEnvironmentVariable(key = "CLIENT_UDP_GLITCHY_OUTBOUND_DROP_MESSAGE_PROBABILITY", value = "0.2")
public class ShootingEventTest extends AbstractGameServerTest {

  @SpyBean
  private AbstractSpawner spawner;

  @SpyBean
  private AntiCheat antiCheat;

  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a running server with 1 connected player
   * @when player 1 shoots and misses
   * @then nobody got shot
   */
  @EnumSource
  @ParameterizedTest
  public void testShootMiss(GameWeaponType gameWeaponType)
      throws IOException, InterruptedException {
    int gameIdToConnectTo = 0;
    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(shooterConnection.getResponse(), 2);
    waitUntilGetResponses(observerConnection.getResponse(), 2);
    emptyQueue(observerConnection.getResponse());
    ServerResponse mySpawn = shooterConnection.getResponse().poll().get();
    int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
    float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(playerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(getWeaponType(gameWeaponType))
        .setDirection(
            Vector.newBuilder().setX(mySpawnEvent.getPlayer().getDirection().getX())
                .setY(mySpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build()).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());
    assertEquals(1, observerConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = observerConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.ATTACK, shootingEvent.getEventType());
    assertEquals(getWeaponType(gameWeaponType), shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(playerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(mySpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(mySpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");

    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(),
        "Full health is nobody got shot(miss)");
    assertFalse(shootingEvent.hasAffectedPlayer(), "Nobody is affected. Missed the shot");
    assertTrue(shooterConnection.getResponse().list().isEmpty(),
        "Shooter shouldn't receive any new messages");
  }

  /**
   * @given a running server with 1 connected player
   * @when player 1 wastes all ammo
   * @then no shooting events are published back to the observers
   */
  @EnumSource
  @ParameterizedTest
  public void testShootMissWasteAllAmmo(GameWeaponType gameWeaponType)
      throws IOException, GameLogicError, InterruptedException {
    if (gameWeaponType.getProjectileType() != null) {
      return;
    }
    int gameIdToConnectTo = 0;
    var weaponInfo = gameRoomRegistry.getGame(gameIdToConnectTo)
        .getRpgWeaponInfo().getWeaponInfo(RPGPlayerClass.WARRIOR, gameWeaponType)
        .get();
    if (weaponInfo.getMaxAmmo() == null) {
      return;
    }
    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(shooterConnection.getResponse(), 2);
    waitUntilGetResponses(observerConnection.getResponse(), 2);
    emptyQueue(observerConnection.getResponse());
    ServerResponse mySpawn = shooterConnection.getResponse().poll().get();
    int playerId = mySpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    var mySpawnEvent = mySpawn.getGameEvents().getEvents(0);
    float newPositionX = mySpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = mySpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());

    var shootingRunnable = new Runnable() {
      @Override
      public void run() {
        shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(playerId)
            .setPingMls(PING_MLS)
            .setGameId(gameIdToConnectTo)
            .setEventType(GameEventType.ATTACK).setWeaponType(getWeaponType(gameWeaponType))
            .setDirection(
                Vector.newBuilder().setX(mySpawnEvent.getPlayer().getDirection().getX())
                    .setY(mySpawnEvent.getPlayer().getDirection().getY()).build())
            .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
            .build());
      }
    };

    // waste all ammo
    for (int i = 0; i < weaponInfo.getMaxAmmo(); i++) {
      shootingRunnable.run();
      waitUntilGetResponses(observerConnection.getResponse(), 1);
      emptyQueue(observerConnection.getResponse());
    }

    // shoot again when ammo is wasted
    shootingRunnable.run();

    // wait a little
    Thread.sleep(2_000);
    assertEquals(0, observerConnection.getResponse().size(),
        "Nothing is expected because all ammo is wasted");
  }


  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2
   * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE and the event is sent
   * to all players
   */
  @RepeatedTest(16)
  public void testShootHit() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    var shotgunInfo = gameRoomRegistry.getGame(gameIdToConnectTo)
        .getRpgWeaponInfo().getWeaponInfo(RPGPlayerClass.WARRIOR, GameWeaponType.SHOTGUN)
        .get();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).getPlayerSpawn(any());

    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(shooterConnection.getResponse(), 2);
    waitUntilGetResponses(getShotConnection.getResponse(), 2);

    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - gameConfig.getDefaultShotgunDamage(),
        shootingEvent.getAffectedPlayer().getHealth());
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        shootingEvent.getAffectedPlayer().getPosition().getX(),
        "Shot player hasn't moved so position has to stay the same");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        shootingEvent.getAffectedPlayer().getPosition().getY(),
        "Shot player hasn't moved so position has to stay the same");

    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }


  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 through a wall
   * @then the event is ignored
   */
  @RepeatedTest(16)
  public void testShootHitCrossingWalls() throws Exception {
    doReturn(true).when(antiCheat).isCrossingWalls(any(), any(), any());

    int gameIdToConnectTo = 0;

    var shotgunInfo = gameRoomRegistry.getGame(gameIdToConnectTo)
        .getRpgWeaponInfo().getWeaponInfo(RPGPlayerClass.WARRIOR, GameWeaponType.SHOTGUN)
        .get();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).getPlayerSpawn(any());

    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(shooterConnection.getResponse(), 2);
    waitUntilGetResponses(getShotConnection.getResponse(), 2);

    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());

    Thread.sleep(2_500);

    // check that nothing happened
    assertEquals(0, shooterConnection.getResponse().size(),
        "No events are expected because it was a gunshot through a wall");
    assertEquals(0, getShotConnection.getResponse().size(),
        "No events are expected because it was a gunshot through a wall");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 standing right in front of him (0.5 distance)
   * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE*3 and the event is sent
   * to all players
   */
  @RepeatedTest(16)
  public void testShootHitVeryClose() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder().x(0.5f).y(0)
                .build()) // standing in front of him
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(15F).build())
        .build()).when(spawner).getPlayerSpawn(any());

    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilGetResponses(getShotConnection.getResponse(), 2);
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - gameConfig.getDefaultShotgunDamage() * 3,
        shootingEvent.getAffectedPlayer().getHealth(),
        "Damage should be amplified because we stand in front of the victim");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        shootingEvent.getAffectedPlayer().getPosition().getX(),
        "Shot player hasn't moved so position has to stay the same");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        shootingEvent.getAffectedPlayer().getPosition().getY(),
        "Shot player hasn't moved so position has to stay the same");
    Thread.sleep(500);
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 standing in front of him (1.5 distance)
   * @then player 2 health is reduced by ServerConfig.DEFAULT_SHOTGUN_DAMAGE*2 and the event is sent
   * to all players
   */
  @RepeatedTest(16)
  public void testShootHitClose() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder().x(1.5f).y(0)
                .build()) // standing in front of him
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).getPlayerSpawn(any());

    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilGetResponses(getShotConnection.getResponse(), 2);
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(getShotConnection.getResponse());
    assertEquals(1, getShotConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - gameConfig.getDefaultShotgunDamage() * 2,
        shootingEvent.getAffectedPlayer().getHealth(),
        "Damage should be amplified because we stand in front of the victim");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        shootingEvent.getAffectedPlayer().getPosition().getX(),
        "Shot player hasn't moved so position has to stay the same");
    assertEquals(shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        shootingEvent.getAffectedPlayer().getPosition().getY(),
        "Shot player hasn't moved so position has to stay the same");
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    emptyQueue(shooterConnection.getResponse());

    shooterConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 punches player 2
   * @then player 2 health is reduced by ServerConfig.DEFAULT_PUNCH_DAMAGE and the event is sent to
   * all players
   */
  @RepeatedTest(16)
  public void testPunchHit() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    var punchingConnection = createGameConnection("localhost", port);
    punchingConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var getPunchedConnection = createGameConnection("localhost", port);
    getPunchedConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(punchingConnection.getResponse());
    ServerResponse puncherPlayerSpawn = punchingConnection.getResponse().poll().get();
    int shooterPlayerId = puncherPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilGetResponses(getPunchedConnection.getResponse(), 2);
    ServerResponse punchedPlayerSpawn = punchingConnection.getResponse().poll().get();
    int punchedPlayerId = punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(getPunchedConnection.getResponse());

    var puncherSpawnEvent = puncherPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = puncherSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = puncherSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(punchingConnection.getResponse());
    punchingConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.PUNCH).setDirection(
            Vector.newBuilder().setX(puncherSpawnEvent.getPlayer().getDirection().getX())
                .setY(puncherSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(punchedPlayerId).build());
    waitUntilQueueNonEmpty(getPunchedConnection.getResponse());
    assertEquals(1, getPunchedConnection.getResponse().size(),
        "Only 1(punching) event is expected");
    ServerResponse serverResponse = getPunchedConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(), "One punch event is expected");
    var punchingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, punchingEvent.getEventType());
    assertEquals(WeaponType.PUNCH, punchingEvent.getWeaponType());
    assertFalse(punchingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, punchingEvent.getPlayer().getPlayerId());

    assertEquals(puncherSpawnEvent.getPlayer().getDirection().getX(),
        punchingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(puncherSpawnEvent.getPlayer().getDirection().getY(),
        punchingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, punchingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, punchingEvent.getPlayer().getPosition().getY());

    assertEquals(100, punchingEvent.getPlayer().getHealth(), "Puncher player health is full");
    assertTrue(punchingEvent.hasAffectedPlayer(), "One player must be punched");
    assertEquals(punchedPlayerId, punchingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(100 - gameConfig.getDefaultPunchDamage(),
        punchingEvent.getAffectedPlayer().getHealth());
    assertEquals(punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getX(),
        punchingEvent.getAffectedPlayer().getPosition().getX(),
        "Punched player hasn't moved so position has to stay the same");
    assertEquals(punchedPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPosition().getY(),
        punchingEvent.getAffectedPlayer().getPosition().getY(),
        "Punched player hasn't moved so position has to stay the same");

    Thread.sleep(500);
    emptyQueue(punchingConnection.getResponse());
    punchingConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(punchingConnection.getResponse());
    var serverInfoResponse = punchingConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "Should be 2 players still");
  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 shoots player 2 too far way
   * @then player 1 event is not published to player 2
   */
  @EnumSource
  @ParameterizedTest
  public void testShootHitTooFarAllWeapons(GameWeaponType weaponType) throws Exception {
    int gameIdToConnectTo = 0;
    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var getShotConnection = createGameConnection("localhost", port);
    getShotConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();
    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);

    Thread.sleep(500);
    emptyQueue(getShotConnection.getResponse());
    emptyQueue(shooterConnection.getResponse());

    var maxDistance = weaponType.getDamageFactory()
        .getDamage(gameRoomRegistry.getGame(gameIdToConnectTo)).getMaxDistance();
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(getWeaponType(weaponType)).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder()
            // too far
            .setX((float) maxDistance * 2)
            .setY((float) maxDistance * 2).build())
        .setAffectedPlayerId(shotPlayerId).build());
    Thread.sleep(1_000);
    assertEquals(0, getShotConnection.getResponse().size(),
        "No response is expected. " + "Actual response: " + getShotConnection.getResponse().list());

    assertTrue(getShotConnection.isConnected());
    assertTrue(shooterConnection.isConnected());
  }


  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2
   * @then player 2 is dead. KILL event is sent to all active players.
   */
  @EnumSource
  @ParameterizedTest
  public void testShootKillAllWeapons(GameWeaponType weaponType) throws Exception {
    int gameIdToConnectTo = 0;
    if (weaponType.getDamageFactory().getDamage(gameRoomRegistry.getGame(gameIdToConnectTo))
        .getDefaultDamage() == null) {
      // skip
      return;
    }
    String shooterPlayerName = "killer";
    var weaponInfo = gameRoomRegistry.getGame(gameIdToConnectTo).getRpgWeaponInfo()
        .getWeaponInfo(RPGPlayerClass.WARRIOR, weaponType).get();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (weaponInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).getPlayerSpawn(any());

    var killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION)
            .setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    var deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION)
            .setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer()
        .getPlayerId();

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotsToKill = (int) Math.ceil(
        100D / weaponType.getDamageFactory().getDamage(gameRoomRegistry.getGame(gameIdToConnectTo))
            .getDefaultDamage());
    for (int i = 0; i < shotsToKill - 1; i++) {
      killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
          .setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo).setEventType(GameEventType.ATTACK)
          .setWeaponType(getWeaponType(weaponType)).setDirection(
              Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
          .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
          .setAffectedPlayerId(shotPlayerId).build());
      waitUntilQueueNonEmpty(deadConnection.getResponse());
      assertTrue(deadConnection.isConnected(), "Player is shot but still alive");
      assertTrue(killerConnection.isConnected(), "Killer must be connected");
      ServerResponse serverResponse = deadConnection.getResponse().poll().get();
      var shootingEvent = serverResponse.getGameEvents().getEvents(0);
      assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
      assertEquals(getWeaponType(weaponType), shootingEvent.getWeaponType());
      assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
      assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
      assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
      assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
      assertEquals(100 -
              weaponType.getDamageFactory().getDamage(gameRoomRegistry.getGame(gameIdToConnectTo))
                  .getDefaultDamage() * (i + 1),
          shootingEvent.getAffectedPlayer().getHealth());
    }
    waitUntilGetResponses(killerConnection.getResponse(), shotsToKill - 1);
    emptyQueue(killerConnection.getResponse());
    // this one kills player 2
    killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(getWeaponType(weaponType)).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, deadShootingEvent.getEventType(),
        "Shot player must be dead");
    assertEquals(getWeaponType(weaponType), deadShootingEvent.getWeaponType());
    assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(getWeaponType(weaponType), killerShootingEvent.getWeaponType());
    assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    killerConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    var serverInfoResponse = killerConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "Must be 2 players");

    String observerPlayerName = "observer";
    var observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION)
            .setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerConnection.getResponse());

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents()
        .getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(shooterPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer");

    assertEquals(shooterPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId,
        observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(shotPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());

  }

  /**
   * @given a running server with 2 connected player
   * @when player 1 kills player 2 and then gets disconnected
   * @then player 2 is dead. KILL event is sent to all active players. player 2 stats are recovered
   * after reconnecting
   */
  @RepeatedTest(16)
  public void testRailgunKillRecovery() throws Exception {
    int gameIdToConnectTo = 0;
    String shooterPlayerName = "killer";
    var killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(shooterPlayerName)
            .setGameId(gameIdToConnectTo).build());

    var deadConnection = createGameConnection("localhost", port);
    deadConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    waitUntilGetResponses(deadConnection.getResponse(), 2);
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse shotPlayerSpawn = killerConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    emptyQueue(deadConnection.getResponse());
    emptyQueue(killerConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;

    killerConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.RAILGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());

    waitUntilQueueNonEmpty(deadConnection.getResponse());
    assertTrue(deadConnection.isConnected(), "Dead players should be connected");
    assertTrue(killerConnection.isConnected(), "Killer must be connected");

    ServerResponse deadPlayerServerResponse = deadConnection.getResponse().poll().get();
    var deadShootingEvent = deadPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, deadShootingEvent.getEventType(),
        "Shot player must be dead");
    assertEquals(WeaponType.RAILGUN, deadShootingEvent.getWeaponType());
    assertFalse(deadShootingEvent.hasLeaderBoard(), "Leader board are published only on spawns");

    assertEquals(shooterPlayerId, deadShootingEvent.getPlayer().getPlayerId());
    assertEquals(100, deadShootingEvent.getPlayer().getHealth(), "Shooter player health is full");
    assertTrue(deadShootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shotPlayerId, deadShootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(0, deadShootingEvent.getAffectedPlayer().getHealth());

    waitUntilQueueNonEmpty(killerConnection.getResponse());
    ServerResponse killerPlayerServerResponse = killerConnection.getResponse().poll().get();
    var killerShootingEvent = killerPlayerServerResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.KILL, killerShootingEvent.getEventType(),
        "Shot player must be dead. Actual response is " + killerPlayerServerResponse);
    assertEquals(WeaponType.RAILGUN, killerShootingEvent.getWeaponType());
    assertEquals(shooterPlayerId, killerShootingEvent.getPlayer().getPlayerId());

    killerConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(killerConnection.getResponse());
    var serverInfoResponse = killerConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "Must be 2 players");

    killerConnection.disconnect();
    Thread.sleep(1_000);

    // reconnect
    killerConnection = createGameConnection("localhost", port);
    killerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setRecoveryPlayerId(shooterPlayerId)
            .setPlayerName(shooterPlayerName).setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(killerConnection.getResponse(), 2);
    var newShooterPlayerSpawn = killerConnection.getResponse().poll().get();
    int newShooterPlayerId = newShooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer()
        .getPlayerId();
    assertNotEquals(newShooterPlayerId, shooterPlayerId,
        "Recovered connection player must have a different id now");

    String observerPlayerName = "observer";
    var observerConnection = createGameConnection("localhost", port);
    observerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName(observerPlayerName)
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(observerConnection.getResponse(), 2);

    var observerPlayerSpawn = observerConnection.getResponse().poll().get().getGameEvents()
        .getEvents(0);
    int observerPlayerId = observerPlayerSpawn.getPlayer().getPlayerId();

    waitUntilQueueNonEmpty(observerConnection.getResponse());
    var spawnEvents = observerConnection.getResponse().poll().get().getGameEvents().getEventsList();
    assertEquals(1, spawnEvents.size(),
        "Should be killer spawn only. Dead players are not to be spawned before respawning. Actual events "
            + spawnEvents);
    assertEquals(ServerResponse.GameEvent.GameEventType.SPAWN, spawnEvents.get(0).getEventType());
    assertEquals(newShooterPlayerId, spawnEvents.get(0).getPlayer().getPlayerId());

    assertTrue(observerPlayerSpawn.hasLeaderBoard(),
        "Newly connected players must have leader board");
    assertEquals(3, observerPlayerSpawn.getLeaderBoard().getItemsCount(),
        "There must be 3 items in the board at this moment: killer, victim, and observer. Current leaderboard: "
            + observerPlayerSpawn.getLeaderBoard());

    assertEquals(newShooterPlayerId,
        observerPlayerSpawn.getLeaderBoard().getItems(0).getPlayerId());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(0).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(0).getDeaths());

    assertEquals(observerPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(1).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getKills());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(1).getDeaths());

    assertEquals(shotPlayerId, observerPlayerSpawn.getLeaderBoard().getItems(2).getPlayerId());
    assertEquals(0, observerPlayerSpawn.getLeaderBoard().getItems(2).getKills());
    assertEquals(1, observerPlayerSpawn.getLeaderBoard().getItems(2).getDeaths());
  }


  /**
   * @given a running server with 2 connected player: 1 active player and 2 dead
   * @when player 1 shoots player 2
   * @then nothing happens as dead players can't get shot
   */
  @RepeatedTest(16)
  public void testShootDeadPlayer() throws Exception {
    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();
    var shotgunInfo = gameRoomRegistry.getGame(gameIdToConnectTo).getRpgWeaponInfo()
        .getWeaponInfo(RPGPlayerClass.WARRIOR, GameWeaponType.SHOTGUN).get();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (shotgunInfo.getMaxDistance()) - 0.5f).y(0).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).getPlayerSpawn(any());
    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    var deadPlayerConnection = createGameConnection("localhost", port);
    deadPlayerConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my other player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(shooterConnection.getResponse(), 2);
    waitUntilGetResponses(deadPlayerConnection.getResponse(), 2);
    emptyQueue(deadPlayerConnection.getResponse());

    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shotPlayerId = shotPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() + 0.1f;
    int shotsToKill = (int) Math.ceil(100D / gameConfig.getDefaultShotgunDamage());
    for (int i = 0; i < shotsToKill; i++) {
      shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
          .setPingMls(PING_MLS)
          .setGameId(gameIdToConnectTo).setEventType(GameEventType.ATTACK)
          .setWeaponType(WeaponType.SHOTGUN).setDirection(
              Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                  .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
          .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
          .setAffectedPlayerId(shotPlayerId).build());
      waitUntilQueueNonEmpty(deadPlayerConnection.getResponse());
      ServerResponse serverResponse = deadPlayerConnection.getResponse().poll().get();
      var shootingEvent = serverResponse.getGameEvents().getEvents(0);
      if (i == shotsToKill - 1) {
        // last shot is a kill
        assertEquals(GameEvent.GameEventType.KILL, shootingEvent.getEventType());
        assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
      } else {
        assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
        assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
      }
      assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());
      assertEquals(100, shootingEvent.getPlayer().getHealth(), "Shooter player health is full");
      assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
      assertEquals(shotPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
      assertEquals(100 - gameConfig.getDefaultShotgunDamage() * (i + 1),
          shootingEvent.getAffectedPlayer().getHealth());
    }
    waitUntilGetResponses(shooterConnection.getResponse(), shotsToKill);
    emptyQueue(shooterConnection.getResponse());
    // shoot dead player
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId).build());
    Thread.sleep(250);
    assertEquals(0, shooterConnection.getResponse().size(),
        "Should be no response as you can't shoot a dead player. Actual response is "
            + shooterConnection.getResponse().list());
    assertEquals(0, shooterConnection.getWarning().size(), "Should be no warnings");
    assertEquals(0, shooterConnection.getErrors().size(),
        "Should be no errors as this situation might happen in a fast paced game");

    emptyQueue(shooterConnection.getResponse());
    shooterConnection.write(
        GetServerInfoCommand.newBuilder().setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    var serverInfoResponse = shooterConnection.getResponse().poll().get();
    List<ServerResponse.GameInfo> games = serverInfoResponse.getServerInfo().getGamesList();
    ServerResponse.GameInfo myGame = games.stream()
        .filter(gameInfo -> gameInfo.getGameId() == gameIdToConnectTo).findFirst().orElseThrow(
            (Supplier<Exception>) () -> new IllegalStateException(
                "Can't find the game we connected to. Actual response :" + serverInfoResponse));
    assertEquals(2, myGame.getPlayersOnline(), "All players must be online");
  }

  /**
   * @given a running server with 1 connected player
   * @when player shoots himself
   * @then player's health is reduced accordingly
   */
  @RepeatedTest(16)
  public void testShootYourself() throws Exception {
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build()).when(spawner).getPlayerSpawn(any());

    int gameIdToConnectTo = 0;
    var gameConfig = gameRoomRegistry.getGame(gameIdToConnectTo).getGameConfig();

    var shooterConnection = createGameConnection("localhost", port);
    shooterConnection.write(
        JoinGameCommand.newBuilder().setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR).setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    ServerResponse shooterPlayerSpawn = shooterConnection.getResponse().poll().get();
    int shooterPlayerId = shooterPlayerSpawn.getGameEvents().getEvents(0).getPlayer().getPlayerId();

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());

    // shoot yourself
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.SHOTGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shooterPlayerId).build());

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    assertEquals(1, shooterConnection.getResponse().size(), "Only 1(shooting) event is expected");
    ServerResponse serverResponse = shooterConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(WeaponType.SHOTGUN, shootingEvent.getWeaponType());
    assertFalse(shootingEvent.hasLeaderBoard(), "No leader board as nobody got killed");
    assertEquals(shooterPlayerId, shootingEvent.getPlayer().getPlayerId());

    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getX(),
        shootingEvent.getPlayer().getDirection().getX(), "Direction shouldn't change");
    assertEquals(shooterSpawnEvent.getPlayer().getDirection().getY(),
        shootingEvent.getPlayer().getDirection().getY(), "Direction shouldn't change");
    assertEquals(newPositionX, shootingEvent.getPlayer().getPosition().getX());
    assertEquals(newPositionY, shootingEvent.getPlayer().getPosition().getY());

    assertEquals(100 - gameConfig.getDefaultShotgunDamage() * 3,
        shootingEvent.getPlayer().getHealth(), "Shooter player health is reduced. "
            + "Damage should be amplified because we are shooting point blank");
    assertTrue(shootingEvent.hasAffectedPlayer(), "One player must be shot");
    assertEquals(shooterPlayerId, shootingEvent.getAffectedPlayer().getPlayerId());
    assertEquals(shootingEvent.getPlayer().getHealth(),
        shootingEvent.getAffectedPlayer().getHealth());

  }
}
