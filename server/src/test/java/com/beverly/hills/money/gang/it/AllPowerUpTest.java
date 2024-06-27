package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.powerup.DefencePowerUp;
import com.beverly.hills.money.gang.powerup.InvisibilityPowerUp;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.QuadDamagePowerUp;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GamePowerUpType;
import com.beverly.hills.money.gang.proto.ServerResponse.PowerUpSpawnEventItem;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_QUAD_DAMAGE_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_QUAD_DAMAGE_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFENCE_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFENCE_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "999999")
public class AllPowerUpTest extends AbstractGameServerTest {

  @SpyBean
  private DefencePowerUp defencePowerUp;

  @SpyBean
  private InvisibilityPowerUp invisibilityPowerUp;

  @SpyBean
  private QuadDamagePowerUp quadDamagePowerUp;

  @Autowired
  private List<PowerUp> allPowerUpBeans;

  /**
   * @given a game with one player
   * @when a player picks up all power-ups
   * @then all power-ups are applied and reverted after some time
   */
  @Test
  public void testPickUpPowerUpAll()
      throws IOException {
    int gameIdToConnectTo = 0;
    GameConnection playerConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost",
        port);
    playerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
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

    var allPowerUps = List.of(GameEventType.INVISIBILITY_POWER_UP, GameEventType.DEFENCE_POWER_UP,
        GameEventType.QUAD_DAMAGE_POWER_UP);

    allPowerUps.forEach(gameEventType -> playerConnection.write(PushGameEventCommand.newBuilder()
        .setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext())
        .setGameId(gameIdToConnectTo)
        .setPosition(PushGameEventCommand.Vector.newBuilder()
            .setX(playerSpawnEvent.getPlayer().getPosition().getX())
            .setY(playerSpawnEvent.getPlayer().getPosition().getY())
            .build())
        .setDirection(PushGameEventCommand.Vector.newBuilder().setX(0).setY(1).build())
        .setEventType(gameEventType)
        .build()));

    waitUntilGetResponses(playerConnection.getResponse(), allPowerUps.size());

    for (PowerUp powerUpBean : allPowerUpBeans) {
      verify(powerUpBean).apply(argThat(playerState -> playerState.getPlayerId() == playerId));
    }
  }
}
