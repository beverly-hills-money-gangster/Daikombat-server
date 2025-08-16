package com.beverly.hills.money.gang.it;

import static com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType.MOVE;
import static com.beverly.hills.money.gang.proto.ServerResponse.GameEvent.GameEventType.SPAWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GamePowerUpType;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEventItem;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class InvisibilityPowerUpTest extends AbstractGameServerTest {


  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a game with one player
   * @when a player picks up invisibility
   * @then invisibility is applied, reverted after GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS, and then
   * released after GAME_SERVER_INVISIBILITY_SPAWN_MLS
   */
  @Test
  public void testPickUpPowerUpInvisibility()
      throws IOException, InterruptedException, GameLogicError {
    int gameIdToConnectTo = 0;
    var game = gameRoomRegistry.getGame(gameIdToConnectTo);
    var invisibilityPowerUp = game.getPowerUpRegistry().get(PowerUpType.INVISIBILITY);
    GameConnection playerConnection = createGameConnection("localhost",
        port);
    playerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN)
            .setPlayerClass(PlayerClass.WARRIOR)
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

    ServerResponse quadDamagePowerUpSpawnResponse = playerConnection.getResponse().poll().get();
    var spawns = quadDamagePowerUpSpawnResponse.getPowerUpSpawn().getItemsList().stream().map(
        PowerUpSpawnEventItem::getType).collect(Collectors.toSet());

    assertEquals(
        Arrays.stream(GamePowerUpType.values()).filter(
                gamePowerUpType -> gamePowerUpType != GamePowerUpType.UNRECOGNIZED)
            .collect(Collectors.toSet()),
        spawns, "All power-ups should be spawned");

    playerConnection.write(PushGameEventCommand.newBuilder()
        .setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setPosition(Vector.newBuilder()
            .setX(playerSpawnEvent.getPlayer().getPosition().getX())
            .setY(playerSpawnEvent.getPlayer().getPosition().getY())
            .build())
        .setDirection(Vector.newBuilder().setX(0).setY(1).build())
        .setEventType(GameEventType.INVISIBILITY_POWER_UP)
        .build());

    waitUntilQueueNonEmpty(playerConnection.getResponse());

    // check that we reverted the power-up
    verify(invisibilityPowerUp).apply(
        argThat(playerState -> playerState.getPlayerId() == playerId));

    ServerResponse powerUpMove = playerConnection.getResponse().poll().get();
    assertEquals(MOVE, powerUpMove.getGameEvents().getEvents(0).getEventType(),
        "After picking up a power-up, we must get a MOVE event with a player having the power-up");
    var playerAfterQuadDamage = powerUpMove.getGameEvents().getEvents(0).getPlayer();
    assertEquals(playerId, playerAfterQuadDamage.getPlayerId());
    assertEquals(1, playerAfterQuadDamage.getActivePowerUpsList().size());
    assertEquals(GamePowerUpType.INVISIBILITY,
        playerAfterQuadDamage.getActivePowerUpsList().get(0).getType());
    assertEquals(invisibilityPowerUp.getLastsForMls(),
        playerAfterQuadDamage.getActivePowerUpsList().get(0).getLastsForMls(), 250);

    Thread.sleep(invisibilityPowerUp.getLastsForMls() + 250);

    // check that we reverted the power-up
    verify(invisibilityPowerUp).revert(
        argThat(playerState -> playerState.getPlayerId() == playerId));

    Thread.sleep(invisibilityPowerUp.getSpawnPeriodMls() + 250);

    assertEquals(1, playerConnection.getResponse().size(),
        "Should be 1 messages: quad damage power-up respawn. Actual response: "
            + playerConnection.getResponse());

    ServerResponse quadDamagePowerUpReSpawnResponse = playerConnection.getResponse().poll().get();
    var quadDamagePowerUpReSpawn = quadDamagePowerUpReSpawnResponse.getPowerUpSpawn();
    assertEquals(GamePowerUpType.INVISIBILITY, quadDamagePowerUpReSpawn.getItems(0).getType());

    GameConnection observerAfterRevert = createGameConnection("localhost",
        port);
    observerAfterRevert.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                PlayerClass.WARRIOR)
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
