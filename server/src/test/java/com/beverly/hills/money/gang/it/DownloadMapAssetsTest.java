package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.DownloadMapAssetsCommand;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.PlayerClass;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.proto.ServerResponse.GameInfo;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@SetEnvironmentVariable(key = "GAME_SERVER_GAMES_TO_CREATE", value = "4")
@SetEnvironmentVariable(key = "0_GAME_SERVER_MAP_NAME", value = "classic")
@SetEnvironmentVariable(key = "1_GAME_SERVER_MAP_NAME", value = "horror")
@SetEnvironmentVariable(key = "2_GAME_SERVER_MAP_NAME", value = "crazy")
@SetEnvironmentVariable(key = "3_GAME_SERVER_MAP_NAME", value = "peace")
public class DownloadMapAssetsTest extends AbstractGameServerTest {

  /**
   * @given a running game server
   * @when a player requests a map
   * @then map assets are returned to the player
   */
  @Test
  public void testDownloadAllMaps() throws IOException {
    GameConnection gameConnection = createGameConnection("localhost", port);
    gameConnection.write(GetServerInfoCommand.newBuilder()
        .setPlayerClass(PlayerClass.WARRIOR).build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    List<GameInfo> games = serverResponse.getServerInfo().getGamesList();
    for (ServerResponse.GameInfo gameInfo : games) {
      gameConnection.write(DownloadMapAssetsCommand.newBuilder()
          .setMapName(gameInfo.getMapMetadata().getName()).build());
      waitUntilQueueNonEmpty(gameConnection.getResponse());
      var response = gameConnection.getResponse().poll().orElseThrow();
      assertTrue(response.hasMapAssets());
      assertTrue(response.getMapAssets().getAtlasPng().size() > 0);
      assertTrue(response.getMapAssets().getOnlineMapTmx().size() > 0);
      assertTrue(response.getMapAssets().getAtlasTsx().size() > 0);
    }
  }


  /**
   * @given a running game server
   * @when a player requests a non-existing map
   * @then an error is returned
   */
  @Test
  public void testDownloadNonExistingMap() throws IOException {
    GameConnection gameConnection = createGameConnection("localhost", port);
    gameConnection.write(DownloadMapAssetsCommand.newBuilder()
        .setMapName("non-existing").build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    var response = gameConnection.getResponse().poll().orElseThrow();
    assertTrue(response.hasErrorEvent());
    assertEquals("Can't find map", response.getErrorEvent().getMessage());
  }
}
