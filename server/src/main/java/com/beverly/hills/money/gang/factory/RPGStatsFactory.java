package com.beverly.hills.money.gang.factory;

import static com.beverly.hills.money.gang.state.entity.PlayerState.VAMPIRE_HP_BOOST;

import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import com.beverly.hills.money.gang.state.PlayerRPGStatValue;
import com.beverly.hills.money.gang.state.PlayerRPGStats;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.util.Map;

public class RPGStatsFactory {


  public static PlayerRPGStats create(RPGPlayerClass playerClass) {
    return switch (playerClass) {
      case WARRIOR -> createDefault();
      case DEMON_TANK -> createTank();
      case ANGRY_SKELETON -> createAngrySkeleton();
    };
  }


  // WARRIOR
  private static PlayerRPGStats createDefault() {
    return PlayerRPGStats.defaultStats();
  }

  // ANGRY SKELETON
  private static PlayerRPGStats createAngrySkeleton() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.GUN_SPEED, PlayerRPGStatValue.createMax()));
  }

  // DEMON TANK
  private static PlayerRPGStats createTank() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.GUN_SPEED, PlayerRPGStatValue.createDefault()));
  }

  public static void main(String[] args) {

    StringBuilder statsBuilder = new StringBuilder();
    for (GameWeaponType gameWeaponType : GameWeaponType.values()) {
      for (RPGPlayerClass attackerClass : RPGPlayerClass.values()) {
        for (RPGPlayerClass victimClass : RPGPlayerClass.values()) {
          var attackerStats = create(attackerClass);
          var victimStats = create(victimClass);
          statsBuilder.append(attackerClass).append(" attacks ").append(victimClass)
              .append(" with a ").append(gameWeaponType).append(": -")
              .append(gameWeaponType.getDefaultDamage() * attackerStats.getNormalized(
                  PlayerRPGStatType.ATTACK) / victimStats.getNormalized(PlayerRPGStatType.DEFENSE))
              .append(". Kill gets HP boost ")
              .append(VAMPIRE_HP_BOOST * attackerStats.getNormalized(PlayerRPGStatType.VAMPIRISM))
              .append("\n");
        }
      }
      statsBuilder.append("\n");
    }
    System.out.println(statsBuilder);
  }

}
