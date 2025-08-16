package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.JoinGameCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.PlayerSkinColor;
import com.beverly.hills.money.gang.proto.PushGameEventCommand;
import com.beverly.hills.money.gang.proto.PushGameEventCommand.GameEventType;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameEvent;
import com.beverly.hills.money.gang.proto.Vector;
import com.beverly.hills.money.gang.registry.GameRoomRegistry;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;

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
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
public class TeleportTest extends AbstractGameServerTest {


  @Autowired
  private GameRoomRegistry gameRoomRegistry;


  /**
   * @given one connected player
   * @when the player enters a teleport
   * @then the player gets teleported
   */

  @Test
  public void testTeleport() throws IOException, GameLogicError {

    int gameIdToConnectTo = 0;
    var game = gameRoomRegistry.getGame(gameIdToConnectTo);
    var teleport1 = game.getTeleportRegistry().getTeleport(0).orElseThrow();
    var teleport2 = game.getTeleportRegistry().getTeleport(1).orElseThrow();

    GameConnection playerConnection = createGameConnection("localhost",
        port);
    playerConnection.write(
        JoinGameCommand.newBuilder()
            .setVersion(ServerConfig.VERSION).setSkin(PlayerSkinColor.GREEN).setPlayerClass(
                PlayerClass.WARRIOR)
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
    assertEquals(game.getTeleportRegistry().getAllTeleports().size(),
        teleportSpawnResponse.getTeleportSpawn().getItemsList().size(),
        "We must get spawns of all teleports after joining the game");

    playerConnection.write(PushGameEventCommand.newBuilder()
        .setPlayerId(playerId)
        .setSequence(sequenceGenerator.getNext()).setPingMls(PING_MLS)
       .setGameId(gameIdToConnectTo)
        .setPosition(Vector.newBuilder()
            .setX(playerSpawnEvent.getPlayer().getPosition().getX())
            .setY(playerSpawnEvent.getPlayer().getPosition().getY())
            .build())
        .setDirection(Vector.newBuilder().setX(0).setY(1).build())
        .setEventType(GameEventType.TELEPORT)
        .setTeleportId(teleport1.getId())
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
    assertEquals(teleport2.getLocation().getX() + teleport2.getDirection().getVector().getX(),
        teleportedPlayer.getPosition().getX());
    assertEquals(teleport2.getLocation().getY() + teleport2.getDirection().getVector().getY(),
        teleportedPlayer.getPosition().getY());
    assertEquals(teleport2.getDirection().getVector().getX(),
        teleportedPlayer.getDirection().getX());
    assertEquals(teleport2.getDirection().getVector().getY(),
        teleportedPlayer.getDirection().getY());
  }
}
