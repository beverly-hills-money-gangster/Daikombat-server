package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.state.entity.PlayerState.VAMPIRE_HP_BOOST;

import com.beverly.hills.money.gang.state.entity.AttackStats;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.util.Map;

public class RPGStatsFactory {


  public static PlayerRPGStats create(RPGPlayerClass playerClass) {
    return switch (playerClass) {
      case COMMONER -> createDefault();
      case TANK -> createTank();
      case BERSERK -> createBerserk();
      case WARRIOR -> createWarrior();
    };
  }


  // COMMONER
  private static PlayerRPGStats createDefault() {
    return PlayerRPGStats.defaultStats();
  }

  // DRACULA BERSERK
  private static PlayerRPGStats createBerserk() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMin(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createDefault()));
  }

  // DEMON TANK
  private static PlayerRPGStats createTank() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createMin()));
  }


  // BEAST WARRIOR
  private static PlayerRPGStats createWarrior() {
    return new PlayerRPGStats(Map.of(
        PlayerRPGStatType.ATTACK, PlayerRPGStatValue.createMax(),
        PlayerRPGStatType.DEFENSE, PlayerRPGStatValue.createDefault(),
        PlayerRPGStatType.VAMPIRISM, PlayerRPGStatValue.createMin()));
  }

  public static void main(String[] args) {

    StringBuilder statsBuilder = new StringBuilder();
    for (AttackType attackType : AttackType.values()) {
      for (RPGPlayerClass attackerClass : RPGPlayerClass.values()) {
        for (RPGPlayerClass victimClass : RPGPlayerClass.values()) {
          var attackerStats = create(attackerClass);
          var victimStats = create(victimClass);
          statsBuilder.append(attackerClass).append(" attacks ").append(victimClass)
              .append(" with a ").append(attackType).append(": -")
              .append(AttackStats.ATTACK_DAMAGE.get(attackType) * attackerStats.getNormalized(
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
