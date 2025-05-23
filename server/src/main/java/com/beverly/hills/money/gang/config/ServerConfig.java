package com.beverly.hills.money.gang.config;


import static com.beverly.hills.money.gang.util.TextUtil.splitCommaSeparatedConfig;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public interface ServerConfig {

  Set<String> BLACKLISTED_WORDS = splitCommaSeparatedConfig(
      System.getenv("GAME_SERVER_BLACKLISTED_WORDS"));

  int GAME_SERVER_PORT = NumberUtils.toInt(System.getenv("GAME_SERVER_PORT"), 7777);
  int VOICE_CHAT_SERVER_PORT = GAME_SERVER_PORT + 1;
  int GAMES_TO_CREATE = NumberUtils.toInt(System.getenv("GAME_SERVER_GAMES_TO_CREATE"), 1);
  int MAX_PLAYERS_PER_GAME = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_PLAYERS_PER_GAME"),
      25);

  int MOVES_UPDATE_FREQUENCY_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS"), 50);

  int MAX_IDLE_TIME_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_IDLE_TIME_MLS"), 10_000);

  int SPAWN_IMMORTAL_MLS = NumberUtils.toInt(
      System.getenv("GAME_SERVER_SPAWN_IMMORTAL_MLS"), 2_000);

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
