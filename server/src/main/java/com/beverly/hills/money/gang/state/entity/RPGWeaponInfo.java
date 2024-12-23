package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.factory.RPGStatsFactory;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RPGWeaponInfo {

  private static final Map<RPGPlayerClass, List<GameWeaponInfo>> WEAPON_INFO = new HashMap<>();

  private static final Map<RPGPlayerClass, List<GameProjectileInfo>> PROJECTILE_INFO = new HashMap<>();

  static {
    Arrays.stream(RPGPlayerClass.values()).forEach(playerClass -> {
      var gunSpeed = RPGStatsFactory.create(playerClass)
          .getNormalized(PlayerRPGStatType.GUN_SPEED);
      var weaponInfo = Arrays.stream(GameWeaponType.values()).map(
              weapon -> GameWeaponInfo
                  .builder()
                  .gameWeaponType(weapon)
                  .delayMls(Math.max((int) (weapon.getAttackDelayMls() / gunSpeed), 150))
                  .maxDistance(weapon.getMaxDistance())
                  .build())
          .collect(Collectors.toList());
      var projectileInfo = Arrays.stream(GameProjectileType.values()).map(
              projectile -> GameProjectileInfo
                  .builder()
                  .gameProjectileType(projectile)
                  .maxDistance(projectile.getMaxDistance())
                  .build())
          .collect(Collectors.toList());
      PROJECTILE_INFO.put(playerClass, projectileInfo);
      WEAPON_INFO.put(playerClass, weaponInfo);
    });

  }

  public static List<GameWeaponInfo> getWeaponsInfo(RPGPlayerClass playerClass) {
    return WEAPON_INFO.get(playerClass);
  }

  public static List<GameProjectileInfo> getProjectilesInfo(RPGPlayerClass playerClass) {
    return PROJECTILE_INFO.get(playerClass);
  }

  public static Optional<GameWeaponInfo> getWeaponInfo(RPGPlayerClass playerClass,
      GameWeaponType weaponType) {
    return getWeaponsInfo(playerClass).stream().filter(
        gameWeaponInfo -> gameWeaponInfo.getGameWeaponType() == weaponType).findFirst();
  }
}
