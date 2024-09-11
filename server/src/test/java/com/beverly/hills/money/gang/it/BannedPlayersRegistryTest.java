package com.beverly.hills.money.gang.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.registry.BannedPlayersRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith({SpringExtension.class, OutputCaptureExtension.class})
@ContextConfiguration(classes = TestConfig.class)
@SetEnvironmentVariable(key = "GAME_SERVER_BAN_TIMEOUT_MLS", value = "5000")
public class BannedPlayersRegistryTest {

  @Autowired
  private BannedPlayersRegistry bannedPlayersRegistry;

  /**
   * @given an empty banned player registry
   * @when isBanned() called
   * @then false is returned because nobody is banned yet
   */
  @Test
  public void testIsBannedNotBanned() {
    assertFalse(bannedPlayersRegistry.isBanned("127.0.0.1"));
  }

  /**
   * @given an empty banned player registry
   * @when isBanned() called
   * @then false is returned because nobody is banned yet
   */
  @Test
  public void testIsBanned() throws InterruptedException {
    String ip = "127.0.0.1";
    bannedPlayersRegistry.ban(ip);
    assertTrue(bannedPlayersRegistry.isBanned(ip));
    Thread.sleep(ServerConfig.BAN_TIMEOUT_MLS + 150);
    assertFalse(bannedPlayersRegistry.isBanned(ip),
        "After ServerConfig.BAN_TIMEOUT_MLS the banned player has to be unbanned");
  }

}
