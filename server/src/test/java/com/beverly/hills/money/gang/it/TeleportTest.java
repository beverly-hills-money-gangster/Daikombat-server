package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.doReturn;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.SkinColorSelection;
import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.PlayerState.PlayerCoordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_QUAD_DAMAGE_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "true")
@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_QUAD_DAMAGE_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFENCE_SPAWN_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFENCE_LASTS_FOR_MLS", value = "2000")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "999999")
@SetEnvironmentVariable(key = "GAME_SERVER_PLAYER_SPEED_CHECK_FREQUENCY_MLS", value = "1000")
public class TeleportTest extends AbstractGameServerTest {


  @MockBean
  private TeleportRegistry teleportRegistry;

  @Autowired
  private Spawner spawner;


  /**
   * @given one connected player
   * @when the player enters a teleport
   * @then the player gets teleported
   */
  @Test
  public void testTeleport() throws IOException, InterruptedException {
    var teleportCoordinates = PlayerCoordinates.builder()
        .direction(Vector.builder().y(1000).x(-1000).build())
        .position(Vector.builder().x(-1).y(1).build())
        .build();
    var mockTeleport = Teleport.builder()
        .id(0).location(TestConfig.MAIN_LOCATION)
        .teleportCoordinates(teleportCoordinates)
        .build();

    doReturn(Optional.of(mockTeleport)).when(teleportRegistry).getTeleport(0);
    doReturn(List.of(mockTeleport)).when(teleportRegistry).getAllTeleports();

    int gameIdToConnectTo = 0;
    GameConnection playerConnection = createGameConnection( "localhost",
        port);
    playerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(SkinColorSelection.GREEN)
            .setPlayerName("my player name")
            .setGameId(gameIdToConnectTo).build());
    waitUntilGetResponses(playerConnection.getResponse(), 2);

    assertEquals(2, playerConnection.getResponse().size(),
        "Should be 2 messages: my spawn + teleports. Actual response: "
            + playerConnection.getResponse());

    ServerResponse playerSpawn = playerConnection.getResponse().poll().get();
    ServerResponse.GameEvent playerSpawnEvent = playerSpawn.getGameEvents()
        .getEvents(0);
    int playerId = playerSpawnEvent.getPlayer().getPlayerId();

    ServerResponse teleportSpawnResponse = playerConnection.getResponse().poll().get();
    var teleportSpawn = teleportSpawnResponse.getTeleportSpawn().getItemsList().stream().findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Expected to get at least one teleport"));

    playerConnection.write(PushGameEventCommand.newBuilder()
        .setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
        .setGameId(gameIdToConnectTo)
        .setPosition(PushGameEventCommand.Vector.newBuilder()
            .setX(playerSpawnEvent.getPlayer().getPosition().getX())
            .setY(playerSpawnEvent.getPlayer().getPosition().getY())
            .build())
        .setDirection(PushGameEventCommand.Vector.newBuilder().setX(0).setY(1).build())
        .setEventType(GameEventType.TELEPORT)
        .setTeleportId(teleportSpawn.getId())
        .build());

    waitUntilGetResponses(playerConnection.getResponse(), 1);

    ServerResponse teleportedPlayerResponse = playerConnection.getResponse().poll().orElseThrow(
        () -> new IllegalStateException("Expected to get teleported player response"));
    var teleportedGameEvent = teleportedPlayerResponse.getGameEvents().getEventsList().stream()
        .filter(
            gameEvent -> gameEvent.getEventType() == GameEvent.GameEventType.TELEPORT).findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No teleport. Actual response is: " + teleportedPlayerResponse));

    var teleportedPlayer = teleportedGameEvent.getPlayer();
    assertEquals(playerId, teleportedPlayer.getPlayerId());
    assertEquals(teleportCoordinates.getPosition().getX(), teleportedPlayer.getPosition().getX());
    assertEquals(teleportCoordinates.getPosition().getY(), teleportedPlayer.getPosition().getY());
    assertEquals(teleportCoordinates.getDirection().getX(), teleportedPlayer.getDirection().getX());
    assertEquals(teleportCoordinates.getDirection().getY(), teleportedPlayer.getDirection().getY());

    // check that we don't get disconnected for cheating
    Thread.sleep(ServerConfig.PLAYER_SPEED_CHECK_FREQUENCY_MLS * 3L);
    assertFalse(playerConnection.isDisconnected(), "Player should stay connected");

  }
}
