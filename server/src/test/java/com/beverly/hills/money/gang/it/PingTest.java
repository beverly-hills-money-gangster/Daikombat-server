package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.SecondaryGameConnection;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_TELEPORTS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_SPAWN_IMMORTAL_MLS", value = "0")
@Disabled("Circle CI fails to run it for some reason")
public class PingTest extends AbstractGameServerTest {


  /**
   * @given running server
   * @when a new connection is created, no request is sent
   * @then it's not disconnected as PING messages are sent automatically
   */
  @Test
  public void testPing() throws IOException, InterruptedException {
    GameConnection gameConnection = createGameConnection( "localhost", port);
    Thread.sleep(5_500);
    assertTrue(gameConnection.isConnected(), "Connection should still be open");
    assertEquals(0, gameConnection.getResponse().size(),
        "We shouldn't get any response as we haven't sent anything yet");
    assertEquals(5, gameConnection.getGameNetworkStats().getSentMessages(),
        "We should have sent 5 PING messages");
    assertTrue(gameConnection.getGameNetworkStats().getPingMls() >= 0);
  }

  /**
   * @given running server
   * @when a new secondary connection is created, no request is sent
   * @then it's not disconnected as PING messages are sent automatically
   */
  @Test
  public void testPingSecondaryConnection() throws IOException, InterruptedException {
    SecondaryGameConnection secondaryGameConnection = createSecondaryGameConnection(
         "localhost", port);
    Thread.sleep(5_500);
    assertTrue(secondaryGameConnection.isConnected(), "Connection should still be open");
    assertEquals(0, secondaryGameConnection.getResponse().size(),
        "We shouldn't get any response as we haven't sent anything yet");
    assertEquals(5, secondaryGameConnection.getGameNetworkStats().getSentMessages(),
        "We should have sent 5 PING messages");
    assertTrue(secondaryGameConnection.getGameNetworkStats().getPingMls() >= 0);
  }
}
