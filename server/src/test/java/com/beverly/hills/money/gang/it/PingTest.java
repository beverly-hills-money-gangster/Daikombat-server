package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.network.GameConnection;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "GAME_SERVER_MAX_IDLE_TIME_MLS", value = "3000")
@SetEnvironmentVariable(key = "GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS", value = "99999")
@SetEnvironmentVariable(key = "CLIENT_MAX_SERVER_INACTIVE_MLS", value = "3000")
public class PingTest extends AbstractGameServerTest {


  /**
   * @given running server
   * @when a new connection is created, no request is sent
   * @then it's not disconnected as PING messages are sent automatically
   */
  @Test
  public void testPing() throws IOException, InterruptedException {
    GameConnection gameConnection = createGameConnection(ServerConfig.PIN_CODE, "localhost", port);
    Thread.sleep(3_500);
    assertTrue(gameConnection.isConnected(), "Connection should still be open");
    assertEquals(0, gameConnection.getResponse().size(),
        "We shouldn't get any response as we haven't sent anything yet");
    assertEquals(3, gameConnection.getNetworkStats().getSentMessages(),
        "We should have sent 3 PING messages");
    assertTrue(gameConnection.getNetworkStats().getPingMls() >= 0);
  }
}
