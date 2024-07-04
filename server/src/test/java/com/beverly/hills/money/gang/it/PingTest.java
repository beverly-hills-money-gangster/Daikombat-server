package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import com.beverly.hills.money.gang.network.SecondaryGameConnection;
import java.io.IOException;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_POWER_UPS_ENABLED", value = "false")
@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "5000")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "5000")
public class PingTest extends AbstractGameServerTest {


  /**
   * @given running server
   * @when a new connection is created, no request is sent
   * @then it's not disconnected as PING messages are sent automatically
   */
  @RepeatedTest(4)
  public void testPing() throws IOException, InterruptedException {
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    Thread.sleep(5_500);
    assertTrue(gameConnection.isConnected(), "Connection should still be open");
    assertEquals(0, gameConnection.getResponse().size(),
        "We shouldn't get any response as we haven't sent anything yet");
    assertEquals(5, gameConnection.getNetworkStats().getSentMessages(),
        "We should have sent 5 PING messages");
    assertTrue(gameConnection.getNetworkStats().getPingMls() >= 0);
  }

  /**
   * @given running server
   * @when a new secondary connection is created, no request is sent
   * @then it's not disconnected as PING messages are sent automatically
   */
  @RepeatedTest(4)
  public void testPingSecondaryConnection() throws IOException, InterruptedException {
    SecondaryGameConnection secondaryGameConnection = createSecondaryGameConnection(
        ServerConfig.PIN_CODE, "localhost", port);
    Thread.sleep(5_500);
    assertTrue(secondaryGameConnection.isConnected(), "Connection should still be open");
    assertEquals(0, secondaryGameConnection.getResponse().size(),
        "We shouldn't get any response as we haven't sent anything yet");
    assertEquals(5, secondaryGameConnection.getNetworkStats().getSentMessages(),
        "We should have sent 5 PING messages");
    assertTrue(secondaryGameConnection.getNetworkStats().getPingMls() >= 0);
  }
}
