package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.entity.PlayerGameId;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_QUAD_DAMAGE_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_HEALTH_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_QUAD_DAMAGE_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFENCE_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFENCE_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "999999")
public class AllPowerUpTest extends AbstractGameServerTest {

  @Autowired
  private GameRoomRegistry gameRoomRegistry;

  /**
   * @given a game with one player
   * @when a player picks up all power-ups
   * @then all power-ups are applied and reverted after some time
   */
  @Test
  public void testPickUpPowerUpAll()
      throws IOException, GameLogicError, InterruptedException {
    int gameIdToConnectTo = 0;
    var playerConnection = createGameConnection("localhost", port);
    var game = gameRoomRegistry.getGame(gameIdToConnectTo);
    var allPowerUps = new ArrayList<PowerUp>();
    for (PowerUpType type : PowerUpType.values()) {
      allPowerUps.add(game.getPowerUpRegistry().get(type));
    }
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

    ServerResponse quadDamagePowerUpSpawnResponse = playerConnection.getResponse().poll().get();
    var spawns = quadDamagePowerUpSpawnResponse.getPowerUpSpawn().getItemsList().stream().map(
        PowerUpSpawnEventItem::getType).collect(Collectors.toSet());

    var powerUpsToPick = Arrays.stream(GamePowerUpType.values()).filter(
            gamePowerUpType -> gamePowerUpType != GamePowerUpType.UNRECOGNIZED)
        .collect(Collectors.toSet());

    assertEquals(powerUpsToPick,
        spawns, "All power-ups should be spawned");

    powerUpsToPick.forEach(powerUpType -> playerConnection.write(PushGameEventCommand.newBuilder()
        .setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setPosition(Vector.newBuilder()
            .setX(playerSpawnEvent.getPlayer().getPosition().getX())
            .setY(playerSpawnEvent.getPlayer().getPosition().getY())
            .build())
        .setDirection(Vector.newBuilder().setX(0).setY(1).build())
        .setEventType(GameEventType.POWER_UP_PICKUP)
        .setPowerUp(powerUpType)
        .build()));

    waitUntilGetResponses(playerConnection.getResponse(), powerUpsToPick.size());

    for (PowerUp powerUpBean : allPowerUps) {
      verify(powerUpBean).apply(argThat(playerState -> playerState.getPlayerId() == playerId));
    }
  }
}
