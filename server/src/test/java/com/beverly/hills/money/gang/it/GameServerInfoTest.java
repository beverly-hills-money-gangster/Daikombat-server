package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameErrorCode;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.proto.GetServerInfoCommand;
import com.beverly.hills.money.gang.proto.ServerResponse;
import com.beverly.hills.money.gang.state.AttackType;
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
public class GameServerInfoTest extends AbstractGameServerTest {

  /**
   * @given a running game serve
   * @when player 1 requests server info
   * @then player 1 gets server info for all games
   */
  @Test
  public void testGetServerInfo() throws IOException {
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);

    gameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    assertTrue(serverResponse.hasServerInfo(), "Must include server info only");
    List<ServerResponse.GameInfo> games = serverResponse.getServerInfo().getGamesList();
    assertEquals(ServerConfig.GAMES_TO_CREATE, games.size());
    assertEquals(ServerConfig.VERSION, serverResponse.getServerInfo().getVersion());
    assertEquals(ServerConfig.MOVES_UPDATE_FREQUENCY_MLS,
        serverResponse.getServerInfo().getMovesUpdateFreqMls());
    assertEquals(ServerConfig.FRAGS_PER_GAME, serverResponse.getServerInfo().getFragsToWin());
    assertEquals(ServerConfig.PLAYER_SPEED, serverResponse.getServerInfo().getPlayerSpeed());
    for (ServerResponse.GameInfo gameInfo : games) {
      assertEquals(ServerConfig.MAX_PLAYERS_PER_GAME, gameInfo.getMaxGamePlayers());
      assertEquals(0, gameInfo.getPlayersOnline(), "Should be no connected players yet");
    }
    assertEquals(AttackType.values().length,
        serverResponse.getServerInfo().getWeaponsInfoList().size(),
        "All attack types/weapons should have info");
    assertEquals(1, gameConnection.getNetworkStats().getReceivedMessages());
    assertTrue(gameConnection.getNetworkStats().getInboundPayloadBytes() > 0);
    assertEquals(1, gameConnection.getNetworkStats().getSentMessages());
    assertTrue(gameConnection.getNetworkStats().getOutboundPayloadBytes() > 0);
  }

  /**
   * @given a running game serve
   * @when player 1 requests server info with incorrect password
   * @then player 1 fails to get server info. server disconnects the player
   */
  @Test
  public void testGetServerInfoBadAuth() throws InterruptedException, IOException {
    GameConnection gameConnection = createGameConnection("wrong password", "localhost", port);
    gameConnection.write(GetServerInfoCommand.newBuilder().build());
    waitUntilQueueNonEmpty(gameConnection.getResponse());
    assertEquals(0, gameConnection.getErrors().size(), "Should be no error");
    assertEquals(1, gameConnection.getResponse().size(), "Should be exactly one response");
    ServerResponse serverResponse = gameConnection.getResponse().poll().get();
    ServerResponse.ErrorEvent errorEvent = serverResponse.getErrorEvent();
    assertEquals(GameErrorCode.AUTH_ERROR.ordinal(), errorEvent.getErrorCode(),
        "Should be auth error");
    assertEquals("Incorrect server pin code", errorEvent.getMessage());
    Thread.sleep(250);
    assertTrue(gameConnection.isDisconnected());
    assertEquals(1, gameConnection.getNetworkStats().getReceivedMessages());
    assertTrue(gameConnection.getNetworkStats().getInboundPayloadBytes() > 0);
    assertEquals(1, gameConnection.getNetworkStats().getSentMessages());
    assertTrue(gameConnection.getNetworkStats().getOutboundPayloadBytes() > 0);
  }

}
