package com.beverly.hills.money.gang.it;

import static com.beverly.hills.money.gang.factory.response.ServerResponseFactory.getWeaponType;
import static com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType.POWER_UP_PICKUP;
import static com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType.SPAWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.GamePowerUpType;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEventItem;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_AMMO_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class BigAmmoPowerUpTest extends AbstractGameServerTest {


  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a game with one player
   * @when a player picks up big ammo
   * @then ammo is fully restored and then released after GAME_SERVER_AMMO_SPAWN_MLS
   */
  @EnumSource(GameWeaponType.class)
  @ParameterizedTest
  public void testPickUpPowerUpBigAmmo(GameWeaponType gameWeaponType)
      throws IOException, InterruptedException, GameLogicError {
    int gameIdToConnectTo = 0;
    var game = gameRoomRegistry.getGame(gameIdToConnectTo);
    var weaponInfo = game.getRpgWeaponInfo().getWeaponInfo(RPGPlayerClass.WARRIOR, gameWeaponType)
        .get();
    var bigAmmoPowerUp = game.getPowerUpRegistry().get(PowerUpType.BIG_AMMO);
    if (weaponInfo.getMaxAmmo() == null) {
      // skip
      return;
    }
    var playerConnection = createGameConnection("localhost",
        port);
    playerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                PlayerClass.WARRIOR)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(playerConnection.getResponse(), 2);

    assertEquals(2, playerConnection.getResponse().size(),
        "Should be 2 messages: my spawn + power up spawn. Actual response: "
            + playerConnection.getResponse());

    ServerResponse playerSpawn = playerConnection.getResponse().poll().get();
    ServerResponse.GameEvent playerSpawnEvent = playerSpawn.getGameEvents()
        .getEvents(0);
    int playerId = playerSpawnEvent.getPlayer().getPlayerId();

    ServerResponse bigAmmoPowerUpSpawnResponse = playerConnection.getResponse().poll().get();
    var spawns = bigAmmoPowerUpSpawnResponse.getPowerUpSpawn().getItemsList().stream().map(
        PowerUpSpawnEventItem::getType).collect(Collectors.toSet());

    assertEquals(
        Arrays.stream(GamePowerUpType.values()).filter(
                gamePowerUpType -> gamePowerUpType != GamePowerUpType.UNRECOGNIZED)
            .collect(Collectors.toSet()),
        spawns, "All power-ups should be spawned");

    for (int i = 0; i < weaponInfo.getMaxAmmo(); i++) {
      playerConnection.write(PushGameEventCommand.newBuilder()
          .setPlayerId(playerId)
          .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
          .setWeaponType(getWeaponType(gameWeaponType))
          .setPosition(Vector.newBuilder()
              .setX(playerSpawnEvent.getPlayer().getPosition().getX())
              .setY(playerSpawnEvent.getPlayer().getPosition().getY())
              .build())
          .setGameId(gameIdToConnectTo)
          .setDirection(Vector.newBuilder().setX(0).setY(1).build())
          .setEventType(GameEventType.ATTACK)
          .build());
    }
    Thread.sleep(1_000);
    playerConnection.write(PushGameEventCommand.newBuilder()
        .setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setPosition(Vector.newBuilder()
            .setX(playerSpawnEvent.getPlayer().getPosition().getX())
            .setY(playerSpawnEvent.getPlayer().getPosition().getY())
            .build())
        .setGameId(gameIdToConnectTo)
        .setDirection(Vector.newBuilder().setX(0).setY(1).build())
        .setEventType(GameEventType.POWER_UP_PICKUP)
        .setPowerUp(GamePowerUpType.BIG_AMMO)
        .build());

    waitUntilQueueNonEmpty(playerConnection.getResponse());

    ServerResponse powerUpMove = playerConnection.getResponse().poll().get();
    assertEquals(POWER_UP_PICKUP, powerUpMove.getGameEvents().getEvents(0).getEventType(),
        "After picking up a power-up, we must get a POWER_UP_PICKUP event with a player having the power-up");
    var playerAfterQuadDamage = powerUpMove.getGameEvents().getEvents(0).getPlayer();
    assertEquals(playerId, playerAfterQuadDamage.getPlayerId());
    var ammo = playerAfterQuadDamage.getCurrentAmmoList().stream().filter(
            playerCurrentWeaponAmmo -> playerCurrentWeaponAmmo.getWeapon()
                .equals(getWeaponType(gameWeaponType)))
        .findFirst().get();
    assertEquals(weaponInfo.getMaxAmmo(), ammo.getAmmo(), "Ammo should be fully restored");
    assertEquals(1, playerAfterQuadDamage.getActivePowerUpsList().size());
    assertEquals(GamePowerUpType.BIG_AMMO,
        playerAfterQuadDamage.getActivePowerUpsList().get(0).getType());
    assertEquals(bigAmmoPowerUp.getLastsForMls(),
        playerAfterQuadDamage.getActivePowerUpsList().get(0).getLastsForMls(), 250);

    Thread.sleep(bigAmmoPowerUp.getSpawnPeriodMls() + 250);

    assertEquals(1, playerConnection.getResponse().size(),
        "Should be 1 message: big ammo power-up respawn. Actual response: "
            + playerConnection.getResponse());

    ServerResponse quadDamagePowerUpReSpawnResponse = playerConnection.getResponse().poll().get();
    var bigAmmoPowerUpReSpawn = quadDamagePowerUpReSpawnResponse.getPowerUpSpawn();
    assertEquals(GamePowerUpType.BIG_AMMO, bigAmmoPowerUpReSpawn.getItems(0).getType());

    var observerAfterRevert = createGameConnection("localhost",
        port);
    observerAfterRevert.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
            .setPlayerName("after revert")
            .setGameId(gameIdToConnectTo).build());
    waitUntilQueueNonEmpty(observerAfterRevert.getResponse());
    Thread.sleep(250);
    assertEquals(3, observerAfterRevert.getResponse().list().size(),
        "Should be 3 messages: my spawn, other player spawn + power-up spawn ");

    ServerResponse otherPlayerSpawn = observerAfterRevert.getResponse().list().get(1);
    var otherPlayerSpawnEvent = otherPlayerSpawn.getGameEvents().getEvents(0);
    assertEquals(SPAWN, otherPlayerSpawnEvent.getEventType());
    assertEquals(playerId, otherPlayerSpawnEvent.getPlayer().getPlayerId());
    assertEquals(0, otherPlayerSpawnEvent.getPlayer().getActivePowerUpsCount(),
        "Should be no power-ups after revert");

  }
}
