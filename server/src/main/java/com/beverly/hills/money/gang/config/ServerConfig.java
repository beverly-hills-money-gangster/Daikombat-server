package com.beverly.hills.money.gang.config;


import java.util.Optional;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public interface ServerConfig {

  int PORT = NumberUtils.toInt(System.getenv("GAME_SERVER_PORT"), 7777);
  int GAMES_TO_CREATE = NumberUtils.toInt(System.getenv("GAME_SERVER_GAMES_TO_CREATE"), 10);
  int MAX_PLAYERS_PER_GAME = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_PLAYERS_PER_GAME"),
      25);

  int BAN_TIMEOUT_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_BAN_TIMEOUT_MLS"),
      30 * 60 * 1000);

  int MOVES_UPDATE_FREQUENCY_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS"), 50);

  int PLAYER_SPEED_CHECK_FREQUENCY_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_PLAYER_SPEED_CHECK_FREQUENCY_MLS"), 10_000);

  int PLAYER_SPEED = NumberUtils.toInt(System.getenv("GAME_SERVER_PLAYER_SPEED"), 7);
  int MAX_IDLE_TIME_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_IDLE_TIME_MLS"), 10_000);

  int DEFAULT_SHOTGUN_DAMAGE = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFAULT_SHOTGUN_DAMAGE"), 20);

  int DEFAULT_RAILGUN_DAMAGE = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFAULT_RAILGUN_DAMAGE"), 75);

  int DEFAULT_MINIGUN_DAMAGE = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFAULT_MINIGUN_DAMAGE"), 5);

  int DEFAULT_PLASMA_DAMAGE = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFAULT_PLASMA_DAMAGE"), 10);

  int DEFAULT_ROCKET_DAMAGE = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFAULT_ROCKET_DAMAGE"), 75);

  int DEFAULT_PUNCH_DAMAGE = NumberUtils.toInt(System.getenv("GAME_SERVER_DEFAULT_PUNCH_DAMAGE"),
      50);

  int QUAD_DAMAGE_SPAWN_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_QUAD_DAMAGE_SPAWN_MLS"), 45_000);

  int QUAD_DAMAGE_LASTS_FOR_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_QUAD_DAMAGE_LASTS_FOR_MLS"), 10_000);

  int DEFENCE_SPAWN_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFENCE_SPAWN_MLS"), 35_000);

  int HEALTH_SPAWN_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_HEALTH_SPAWN_MLS"), 35_000);

  int DEFENCE_LASTS_FOR_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_DEFENCE_LASTS_FOR_MLS"), 10_000);

  int INVISIBILITY_SPAWN_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_INVISIBILITY_SPAWN_MLS"), 30_000);

  int INVISIBILITY_LASTS_FOR_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_INVISIBILITY_LASTS_FOR_MLS"), 15_000);

  boolean FAST_TCP = Boolean.parseBoolean(StringUtils.defaultIfBlank(
      System.getenv("GAME_SERVER_FAST_TCP"), "true"));

  boolean POWER_UPS_ENABLED = Boolean.parseBoolean(StringUtils.defaultIfBlank(
      System.getenv("GAME_SERVER_POWER_UPS_ENABLED"), "true"));

  boolean TELEPORTS_ENABLED = Boolean.parseBoolean(StringUtils.defaultIfBlank(
      System.getenv("GAME_SERVER_TELEPORTS_ENABLED"), "true"));

  int FRAGS_PER_GAME = NumberUtils.toInt(System.getenv("GAME_SERVER_FRAGS_PER_GAME"), 25);
  int PUNCH_DELAY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_PUNCH_DELAY_MLS"), 300);
  int SHOTGUN_DELAY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_SHOTGUN_DELAY_MLS"), 450);
  int RAILGUN_DELAY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_RAILGUN_DELAY_MLS"), 1_700);
  int ROCKET_LAUNCHER_DELAY_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_ROCKET_LAUNCHER_DELAY_MLS"), 1_500);
  int PLASMAGUN_DELAY_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_PLASMAGUN_DELAY_MLS"), 175);
  int MINIGUN_DELAY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MINIGUN_DELAY_MLS"), 155);

  int PLAYER_STATS_TIMEOUT_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_PLAYER_STATS_TIMEOUT_MLS"), 2 * 60 * 1000);

  String VERSION = Optional.ofNullable(
      ServerConfig.class.getClassLoader().getResourceAsStream("server-version.properties")).map(
      inputStream -> {
        try (inputStream) {
          Properties properties = new Properties();
          properties.load(inputStream);
          return properties.getProperty("server.version");
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).orElseThrow(
      () -> new IllegalStateException("Can't get version"));

}
