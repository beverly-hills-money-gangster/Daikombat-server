package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.factory.RPGStatsFactory;
import com.beverly.hills.money.gang.state.AttackType;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AttackStats {

  public static final Map<AttackType, Double> MAX_ATTACK_DISTANCE = Map.of(
      AttackType.PUNCH, 1.2,
      AttackType.SHOTGUN, 7.0,
      AttackType.MINIGUN, 7.0,
      AttackType.RAILGUN, 10.0);

  public static final Map<AttackType, Integer> ATTACK_DELAY_MLS = Map.of(
      AttackType.PUNCH, ServerConfig.PUNCH_DELAY_MLS,
      AttackType.SHOTGUN, ServerConfig.SHOTGUN_DELAY_MLS,
      AttackType.RAILGUN, ServerConfig.RAILGUN_DELAY_MLS,
      AttackType.MINIGUN, ServerConfig.MINIGUN_DELAY_MLS);

  public static final Map<AttackType, Integer> ATTACK_DAMAGE = Map.of(
      AttackType.PUNCH, ServerConfig.DEFAULT_PUNCH_DAMAGE,
      AttackType.SHOTGUN, ServerConfig.DEFAULT_SHOTGUN_DAMAGE,
      AttackType.RAILGUN, ServerConfig.DEFAULT_RAILGUN_DAMAGE,
      AttackType.MINIGUN, ServerConfig.DEFAULT_MINIGUN_DAMAGE);

  private static final Map<RPGPlayerClass, List<AttackInfo>> ATTACKS_INFO = new HashMap<>();

  static {
    if (MAX_ATTACK_DISTANCE.size() != AttackType.values().length) {
      throw new IllegalStateException("Not all attack types have max distance");
    }
    if (ATTACK_DELAY_MLS.size() != AttackType.values().length) {
      throw new IllegalStateException("Not all attack types have delay");
    }
    if (ATTACK_DAMAGE.size() != AttackType.values().length) {
      throw new IllegalStateException("Not all attack types have damage configured");
    }

    Arrays.stream(RPGPlayerClass.values()).forEach(playerClass -> {
      var gunSpeed = RPGStatsFactory.create(playerClass)
          .getNormalized(PlayerRPGStatType.GUN_SPEED);
      var info = Arrays.stream(AttackType.values()).map(
              attackType -> AttackInfo
                  .builder()
                  .attackType(attackType)
                  .delayMls(Math.max((int) (ATTACK_DELAY_MLS.get(attackType) / gunSpeed), 150))
                  .maxDistance(MAX_ATTACK_DISTANCE.get(attackType))
                  .defaultDamage(ATTACK_DAMAGE.get(attackType))
                  .build())
          .collect(Collectors.toList());
      ATTACKS_INFO.put(playerClass, info);
    });
  }

  public static List<AttackInfo> getAttacksInfo(RPGPlayerClass playerClass) {
    return ATTACKS_INFO.get(playerClass);
  }
}
