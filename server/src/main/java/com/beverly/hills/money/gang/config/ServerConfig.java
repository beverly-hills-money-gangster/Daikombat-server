package com.beverly.hills.money.gang.config;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Optional;
import java.util.Properties;

public interface ServerConfig {

    int GAMES_TO_CREATE = NumberUtils.toInt(System.getenv("GAMES_TO_CREATE"), 10);
    int MAX_PLAYERS_PER_GAME = NumberUtils.toInt(System.getenv("MAX_PLAYERS_PER_GAME"), 25);
    int MOVES_UPDATE_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("MOVES_UPDATE_FREQUENCY_MLS"), 50);
    int IDLE_PLAYERS_KILLER_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("IDLE_PLAYERS_KILLER_FREQUENCY_MLS"), 10_000);
    int MAX_IDLE_TIME_MLS = NumberUtils.toInt(System.getenv("MAX_IDLE_TIME_MLS"), 10_000);
    int DEFAULT_DAMAGE = NumberUtils.toInt(System.getenv("DEFAULT_DAMAGE"), 20);
    String PASSWORD = StringUtils.defaultIfBlank(System.getenv("PASSWORD"), "daikombat");

    String VERSION = Optional.ofNullable(
            ServerConfig.class.getClassLoader().getResourceAsStream("version.properties")).map(
            inputStream -> {
                try (inputStream) {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    return properties.getProperty("version");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).orElseThrow(
            () -> new IllegalStateException("Can't get version"));

}
