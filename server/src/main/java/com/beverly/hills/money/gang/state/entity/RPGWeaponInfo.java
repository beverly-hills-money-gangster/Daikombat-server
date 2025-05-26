package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.factory.rpg.RPGStatsFactory;
import com.beverly.hills.money.gang.state.GameProjectileType;
import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RPGWeaponInfo {

  private final Map<RPGPlayerClass, List<GameWeaponInfo>> WEAPON_INFO = new HashMap<>();

  private final Map<RPGPlayerClass, List<GameProjectileInfo>> PROJECTILE_INFO = new HashMap<>();

  public RPGWeaponInfo(GameReader gameReader) {
    Arrays.stream(RPGPlayerClass.values()).forEach(playerClass -> {
      var gunSpeed = RPGStatsFactory.create(playerClass)
          .getNormalized(PlayerRPGStatType.GUN_SPEED);
      var weaponInfo = Arrays.stream(GameWeaponType.values()).map(
              weapon -> {
                var damage = weapon.getDamageFactory().getDamage(gameReader);
                return GameWeaponInfo
                    .builder()
                    .gameWeaponType(weapon)
                    .delayMls(Math.max((int) (damage.getAttackDelayMls() / gunSpeed), 150))
                    .maxDistance(damage.getMaxDistance())
                    .build();
              })
          .collect(Collectors.toList());
      var projectileInfo = Arrays.stream(GameProjectileType.values()).map(
              projectile -> {
                var damage = projectile.getDamageFactory().getDamage(gameReader);
                return GameProjectileInfo
                    .builder()
                    .gameProjectileType(projectile)
                    .maxDistance(damage.getMaxDistance())
                    .build();
              })
          .collect(Collectors.toList());
      PROJECTILE_INFO.put(playerClass, projectileInfo);
      WEAPON_INFO.put(playerClass, weaponInfo);
    });
  }

  public List<GameWeaponInfo> getWeaponsInfo(RPGPlayerClass playerClass) {
    return WEAPON_INFO.get(playerClass);
  }

  public List<GameProjectileInfo> getProjectilesInfo(RPGPlayerClass playerClass) {
    return PROJECTILE_INFO.get(playerClass);
  }

  public Optional<GameWeaponInfo> getWeaponInfo(RPGPlayerClass playerClass,
      GameWeaponType weaponType) {
    return getWeaponsInfo(playerClass).stream().filter(
        gameWeaponInfo -> gameWeaponInfo.getGameWeaponType() == weaponType).findFirst();
  }
}
