package com.beverly.hills.money.gang.config;


import org.apache.commons.lang3.math.NumberUtils;

public interface GameConfig {

    int GAMES_TO_CREATE = NumberUtils.toInt(System.getenv("GAMES_TO_CREATE"), 10);
    int MAX_PLAYERS_PER_GAME = NumberUtils.toInt(System.getenv("MAX_PLAYERS_PER_GAME"), 25);
    int MOVES_UPDATE_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("MOVES_UPDATE_FREQUENCY_MLS"), 200);
    int IDLE_PLAYERS_KILLER_FREQUENCY_MLS = NumberUtils.toInt(System.getenv("IDLE_PLAYERS_KILLER_FREQUENCY_MLS"), 10_000);
    int MAX_IDLE_TIME_MLS = NumberUtils.toInt(System.getenv("IDLE_PLAYERS_KILLER_FREQUENCY_MLS"), 30_000);
    int DEFAULT_DAMAGE = NumberUtils.toInt(System.getenv("DEFAULT_DAMAGE"), 20);

}
