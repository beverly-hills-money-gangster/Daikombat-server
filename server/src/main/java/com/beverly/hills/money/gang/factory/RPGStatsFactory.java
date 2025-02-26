package com.beverly.hills.money.gang.factory;

import static com.beverly.hills.money.gang.state.entity.PlayerState.DEFAULT_VAMPIRE_HP_BOOST;

import com.beverly.hills.money.gang.state.GameWeaponType;
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
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.GUN_SPEED, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.RUN_SPEED, PlayerRPGStatValue.create(130)));
  }

  // DEMON TANK
  private static PlayerRPGStats createTank() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.GUN_SPEED, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.RUN_SPEED, PlayerRPGStatValue.create(90)));
  }

  public static void main(String[] args) {

    StringBuilder statsBuilder = new StringBuilder();
    for (GameWeaponType gameWeaponType : GameWeaponType.values()) {
      for (RPGPlayerClass attackerClass : RPGPlayerClass.values()) {
        var attackerStats = create(attackerClass);
        for (RPGPlayerClass victimClass : RPGPlayerClass.values()) {
          var victimStats = create(victimClass);
          statsBuilder.append(attackerClass).append(" attacks ").append(victimClass)
              .append(" with a ").append(gameWeaponType).append(": -")
              .append(gameWeaponType.getDefaultDamage() * attackerStats.getNormalized(
                  PlayerRPGStatType.ATTACK) / victimStats.getNormalized(PlayerRPGStatType.DEFENSE))
              .append(". Kill gets HP boost ")
              .append(DEFAULT_VAMPIRE_HP_BOOST * attackerStats.getNormalized(PlayerRPGStatType.VAMPIRISM))
              .append("\n");
        }
      }
      statsBuilder.append("\n");
    }
    System.out.println(statsBuilder);
  }

}
