package com.beverly.hills.money.gang.config;

import java.util.Optional;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;


@ToString
public class GameRoomServerConfig {

  private final Integer gameRoomId;
  @Delegate
  private final GameRoomServerConfigHolder gameRoomServerConfigHolder;

  public GameRoomServerConfig(final Integer gameRoomId) {
    this.gameRoomId = gameRoomId;
    this.gameRoomServerConfigHolder = new GameRoomServerConfigHolder();
  }

  @Getter
  public class GameRoomServerConfigHolder {

    private final String title = ObjectUtils.defaultIfNull(getRoomEnv(
        "GAME_SERVER_ROOM_TITLE"), "");
    private final String description = ObjectUtils.defaultIfNull(
        getRoomEnv("GAME_SERVER_ROOM_DESCRIPTION"), "");
    private final int defaultShotgunDamage = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE"), 20);
    private final int defaultRailgunDamage = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_DEFAULT_RAILGUN_DAMAGE"), 75);
    private final int defaultMinigunDamage = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_DEFAULT_MINIGUN_DAMAGE"), 5);
    private final int defaultPlasmaDamage = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_DEFAULT_PLASMA_DAMAGE"), 10);
    private final int defaultRocketDamage = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_DEFAULT_ROCKET_DAMAGE"), 75);
    private final int defaultPunchDamage = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_DEFAULT_PUNCH_DAMAGE"), 50);
    private final int punchDelayMls = NumberUtils.toInt(getRoomEnv(
        "GAME_SERVER_PUNCH_DELAY_MLS"), 300);
    private final int shotgunDelayMls = NumberUtils.toInt(getRoomEnv(
        "GAME_SERVER_SHOTGUN_DELAY_MLS"), 450);
    private final int railgunDelayMls = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_RAILGUN_DELAY_MLS"), 1_700);
    private final int rocketLauncherDelayMls = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_ROCKET_LAUNCHER_DELAY_MLS"), 1_500);
    private final int plasmagunDelayMls = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_PLASMAGUN_DELAY_MLS"), 155);
    private final int minigunDelayMls = NumberUtils.toInt(getRoomEnv(
        "GAME_SERVER_MINIGUN_DELAY_MLS"), 155);
    private final int playerSpeed = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_PLAYER_SPEED"), 7);
    private final int maxVisibility = NumberUtils.toInt(
        getRoomEnv("GAME_SERVER_MAX_VISIBILITY"), 14);

  }

  String getRoomEnv(final String envName) {
    return Optional.ofNullable(System.getenv(formatEnvName(envName)))
        .orElse(System.getenv(envName));
  }

  private String formatEnvName(final String envName) {
    if (gameRoomId == null) {
      throw new IllegalStateException("Game room is not initialized");
    }
    return String.format("%s_" + envName, gameRoomId);
  }
}
