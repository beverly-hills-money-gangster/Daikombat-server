package com.beverly.hills.money.gang.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@SetEnvironmentVariable(key = "SOME_ENV_VAR_THAT_EMPTY", value = "")
@SetEnvironmentVariable(key = "0_GAME_ROOM_ENV_VAR_POPULATED", value = "ABC")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE", value = "50")
@SetEnvironmentVariable(key = "0_GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE", value = "75")
@SetEnvironmentVariable(key = "1_GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE", value = "5")
@SetEnvironmentVariable(key = "GAME_SERVER_DEFAULT_RAILGUN_DAMAGE", value = "99")
public class GameRoomServerConfigTest {

  private final GameRoomServerConfig gameRoomServerConfig0 = new GameRoomServerConfig(0);
  private final GameRoomServerConfig gameRoomServerConfig1 = new GameRoomServerConfig(1);

  @Test
  public void testGetRoomEnvNotSet() {
    assertNull(gameRoomServerConfig0.getRoomEnv("SOME_ENV_VAR_THAT_NOT_SET"));
  }

  @Test
  public void testGetRoomEnvDefault() {
    assertEquals("99",
        gameRoomServerConfig0.getRoomEnv("GAME_SERVER_DEFAULT_RAILGUN_DAMAGE"),
        "Game room specific var is not set so it should be taken from common configs");
  }

  @Test
  public void testGetRoomEnvRoomSpecific() {
    assertEquals("75",
        gameRoomServerConfig0.getRoomEnv("GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE"),
        "Game room specific var is set so it should be preferred");
  }

  @Test
  public void testGetRoomEnvRoomSpecificRoom1() {
    assertEquals("5",
        gameRoomServerConfig1.getRoomEnv("GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE"),
        "Game room specific var is set so it should be preferred");
  }
}
