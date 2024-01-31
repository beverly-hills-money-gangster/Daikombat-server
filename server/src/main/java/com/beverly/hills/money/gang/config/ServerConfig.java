package com.beverly.hills.money.gang.config;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;
import java.util.Properties;

public interface ServerConfig {

    int PORT = NumberUtils.toInt(System.getenv("GAME_SERVER_PORT"), 7777);
    int GAMES_TO_CREATE = NumberUtils.toInt(System.getenv("GAME_SERVER_GAMES_TO_CREATE"), 10);
    int MAX_PLAYERS_PER_GAME = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_PLAYERS_PER_GAME"), 25);
    int MOVES_UPDATE_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MOVES_UPDATE_FREQUENCY_MLS"), 50);
    int PING_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_PING_FREQUENCY_MLS"), 2_500);
    int IDLE_PLAYERS_KILLER_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_IDLE_PLAYERS_KILLER_FREQUENCY_MLS"), 10_000);
    int MAX_IDLE_TIME_MLS = NumberUtils.toInt(System.getenv("GAME_SERVER_MAX_IDLE_TIME_MLS"), 10_000);
    int DEFAULT_DAMAGE = NumberUtils.toInt(System.getenv("GAME_SERVER_DEFAULT_DAMAGE"), 20);
    String PASSWORD = StringUtils.defaultIfBlank(System.getenv("GAME_SERVER_PASSWORD"), "daikombat");

    String VERSION = Optional.ofNullable(
            ServerConfig.class.getClassLoader().getResourceAsStream("version.properties")).map(
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
