package com.beverly.hills.money.gang.factory.rpg;

import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import com.beverly.hills.money.gang.state.PlayerRPGStatValue;
import com.beverly.hills.money.gang.state.PlayerRPGStats;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.util.HashMap;
import java.util.Map;

public class RPGStatsFactory {


  private static final Map<RPGPlayerClass, PlayerRPGStats> STATS = new HashMap<>();

  static {
    for (RPGPlayerClass playerClass : RPGPlayerClass.values()) {
      var stats = switch (playerClass) {
        case WARRIOR -> createDefault();
        case DEMON_TANK -> createTank();
        case ANGRY_SKELETON -> createAngrySkeleton();
      };
      STATS.put(playerClass, stats);
    }

  }

  public static PlayerRPGStats create(final RPGPlayerClass playerClass) {
    return STATS.get(playerClass);
  }

  // WARRIOR
  private static PlayerRPGStats createDefault() {
    return defaultStats();
  }

  // default stats
  private static PlayerRPGStats defaultStats() {
    Map<PlayerRPGStatType, PlayerRPGStatValue> stats = new HashMap<>();
    for (var statType : PlayerRPGStatType.values()) {
      stats.put(statType, PlayerRPGStatValue.createDefault());
    }
    return new PlayerRPGStats(stats);
  }

  // ANGRY SKELETON
  private static PlayerRPGStats createAngrySkeleton() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.create(200),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.GUN_SPEED, PlayerRPGStatValue.create(165),
        PlayerRPGStatType.RUN_SPEED, PlayerRPGStatValue.create(120)));
  }

  // DEMON TANK
  private static PlayerRPGStats createTank() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.GUN_SPEED, PlayerRPGStatValue.create(90),
        PlayerRPGStatType.RUN_SPEED, PlayerRPGStatValue.create(85)));
  }

}
