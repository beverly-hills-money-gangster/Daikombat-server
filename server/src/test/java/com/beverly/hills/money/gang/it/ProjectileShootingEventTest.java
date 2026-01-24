package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.beverly.hills.money.gang.cheat.AntiCheat;
import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.ProjectileCoordinates;
import com.beverly.hills.money.gang.proto.ProjectileType;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.proto.WeaponType;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.RepeatedTest;
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
public class ProjectileShootingEventTest extends AbstractGameServerTest {

  @SpyBean
  private AbstractSpawner spawner;

  @SpyBean
  private AntiCheat antiCheat;

  @Autowired
  private GameRoomRegistry gameRoomRegistry;


  /**
   * @given a running server with 2 connected player
   * @when player 1 launcher a rocket at player 2
   * @then player 2 health is reduced and the event is sent to all players
   */
  @RepeatedTest(16)
  public void testShootHitRocketLauncher() throws Exception {

    int gameIdToConnectTo = 0;
    var rocketLauncherInfo = gameRoomRegistry.getGame(gameIdToConnectTo)
        .getRpgWeaponInfo().getWeaponInfo(RPGPlayerClass.WARRIOR,
            GameWeaponType.ROCKET_LAUNCHER)
        .get();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (rocketLauncherInfo.getMaxDistance()) - 0.5f).y(0).build())
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

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    waitUntilGetResponses(getShotConnection.getResponse(), 2);
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    var shotSpawnEvent = shotPlayerSpawn.getGameEvents().getEvents(0);
    float shotPositionX = shotSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float shotPositionY = shotSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotPlayerId = shotSpawnEvent.getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());

    // launch a rocket
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.ROCKET_LAUNCHER).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .build());

    // projectile is flying
    Thread.sleep(500);
    // blow up the rocket
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK)
        .setProjectile(ProjectileCoordinates.newBuilder()
            .setBlowUpPosition(Vector.newBuilder().setX(shotPositionX).setY(shotPositionY).build())
            .setProjectileType(ProjectileType.ROCKET).build())
        .setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId)
        .build());

    waitUntilGetResponses(getShotConnection.getResponse(), 2);
    assertEquals(2, getShotConnection.getResponse().size(),
        "2 (launch + rocket) events are expected. Actual response is: "
            + getShotConnection.getResponse().list());

    ServerResponse launchResponse = getShotConnection.getResponse().poll().get();
    var launchEvent = launchResponse.getGameEvents().getEvents(0);
    assertTrue(launchResponse.hasGameEvents(), "A game event is expected");
    assertEquals(GameEvent.GameEventType.ATTACK, launchEvent.getEventType());
    assertEquals(WeaponType.ROCKET_LAUNCHER, launchEvent.getWeaponType());

    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(ProjectileType.ROCKET, shootingEvent.getProjectile().getProjectileType());
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
    assertEquals(100 - GameProjectileType.ROCKET
            .getDamageFactory().getDamage(gameRoomRegistry.getGame(gameIdToConnectTo))
            .getDefaultDamage(),
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
   * @when player 1 shoots plasma at player 2
   * @then player 2 health is reduced and the event is sent to all players
   */
  @RepeatedTest(16)
  public void testShootHitPlasmagun() throws Exception {
    int gameIdToConnectTo = 0;
    var plasmagunInfo = gameRoomRegistry.getGame(gameIdToConnectTo).getRpgWeaponInfo()
        .getWeaponInfo(RPGPlayerClass.WARRIOR, GameWeaponType.PLASMAGUN)
        .get();
    doReturn(Coordinates.builder()
        .position(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(0F).build())
        .direction(com.beverly.hills.money.gang.state.entity.Vector.builder().x(0F).y(1F).build())
        .build(), Coordinates.builder().position(
            com.beverly.hills.money.gang.state.entity.Vector.builder()
                .x((float) (plasmagunInfo.getMaxDistance()) - 0.5f).y(0).build())
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

    waitUntilQueueNonEmpty(shooterConnection.getResponse());
    waitUntilGetResponses(getShotConnection.getResponse(), 2);
    ServerResponse shotPlayerSpawn = shooterConnection.getResponse().poll().get();
    var shotSpawnEvent = shotPlayerSpawn.getGameEvents().getEvents(0);
    float shotPositionX = shotSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float shotPositionY = shotSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    int shotPlayerId = shotSpawnEvent.getPlayer().getPlayerId();

    emptyQueue(getShotConnection.getResponse());

    var shooterSpawnEvent = shooterPlayerSpawn.getGameEvents().getEvents(0);
    float newPositionX = shooterSpawnEvent.getPlayer().getPosition().getX() + 0.1f;
    float newPositionY = shooterSpawnEvent.getPlayer().getPosition().getY() - 0.1f;
    emptyQueue(shooterConnection.getResponse());

    // launch plasma
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK).setWeaponType(WeaponType.PLASMAGUN).setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .build());

    // projectile is flying
    Thread.sleep(500);

    // blow up plasma
    shooterConnection.write(PushGameEventCommand.newBuilder().setPlayerId(shooterPlayerId)
        .setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setEventType(GameEventType.ATTACK)
        .setProjectile(ProjectileCoordinates.newBuilder()
            .setBlowUpPosition(Vector.newBuilder().setX(shotPositionX).setY(shotPositionY).build())
            .setProjectileType(ProjectileType.PLASMA).build())
        .setDirection(
            Vector.newBuilder().setX(shooterSpawnEvent.getPlayer().getDirection().getX())
                .setY(shooterSpawnEvent.getPlayer().getDirection().getY()).build())
        .setPosition(Vector.newBuilder().setX(newPositionX).setY(newPositionY).build())
        .setAffectedPlayerId(shotPlayerId)
        .build());

    waitUntilGetResponses(getShotConnection.getResponse(), 2);
    assertEquals(2, getShotConnection.getResponse().size(),
        "2 (launch + plasma) events are expected. Actual response is: "
            + getShotConnection.getResponse().list());

    var launchResponse = getShotConnection.getResponse().poll().get();
    var launchEvent = launchResponse.getGameEvents().getEvents(0);
    assertTrue(launchResponse.hasGameEvents(), "A game event is expected");
    assertEquals(GameEvent.GameEventType.ATTACK, launchEvent.getEventType());
    assertEquals(WeaponType.PLASMAGUN, launchEvent.getWeaponType());

    ServerResponse serverResponse = getShotConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasGameEvents(), "A game event is expected");
    assertEquals(1, serverResponse.getGameEvents().getEventsCount(),
        "One shooting event is expected");
    var shootingEvent = serverResponse.getGameEvents().getEvents(0);
    assertEquals(GameEvent.GameEventType.GET_ATTACKED, shootingEvent.getEventType());
    assertEquals(ProjectileType.PLASMA, shootingEvent.getProjectile().getProjectileType());
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
    assertEquals(100 - GameProjectileType.PLASMA.getDamageFactory()
            .getDamage(gameRoomRegistry.getGame(gameIdToConnectTo)).getDefaultDamage(),
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

}
