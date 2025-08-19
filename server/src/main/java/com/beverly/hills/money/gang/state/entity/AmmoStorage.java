package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.GameWeaponType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AmmoStorage implements AmmoStorageReader {

  private final Map<GameWeaponType, WeaponAmmo> storage = new ConcurrentHashMap<>();

  private final GameReader gameReader;

  private final Set<GameWeaponType> availableWeapons;

  public AmmoStorage(final GameReader gameReader, final RPGPlayerClass playerClass) {
    this.gameReader = gameReader;
    this.availableWeapons = playerClass.getWeapons();
    for (GameWeaponType weaponType : GameWeaponType.values()) {
      if (!availableWeapons.contains(weaponType)) {
        // if weapon is not supported, then it's 0 ammo
        storage.put(weaponType, new WeaponAmmo(0));
        continue;
      }
      Optional.ofNullable(weaponType.getDamageFactory().getDamage(gameReader).getMaxAmmo())
          .ifPresent(
              maxAmmo -> storage.put(weaponType, new WeaponAmmo(maxAmmo)));
    }
  }

  public void restore(final GameWeaponType weaponType, final float ratio) {
    if (ratio <= 0) {
      return;
    }
    if (!availableWeapons.contains(weaponType)) {
      return;
    }
    Optional.ofNullable(storage.get(weaponType)).ifPresent(weaponAmmo -> {
      int currentAmmo = weaponAmmo.getCurrentAmmo();
      Optional.ofNullable(weaponType.getDamageFactory().getDamage(gameReader).getMaxAmmo())
          .ifPresent(
              maxAmmo -> storage.put(weaponType,
                  new WeaponAmmo(
                      Math.min(maxAmmo, currentAmmo + (int) (maxAmmo * Math.abs(ratio))))));
    });
  }

  public boolean wasteAmmo(final GameWeaponType weaponType) {
    return Optional.ofNullable(storage.get(weaponType))
        .map(WeaponAmmo::wasteAmmo)
        // if ammo is not specified(like melee), then we let it go
        .orElse(true);
  }


  public Integer getCurrentAmmo(GameWeaponType gameWeaponType) {
    return Optional.ofNullable(storage.get(gameWeaponType))
        .map(WeaponAmmo::getCurrentAmmo).orElse(null);
  }

  @Override
  public Map<GameWeaponType, Integer> getCurrentAmmo() {
    Map<GameWeaponType, Integer> currentAmmo = new HashMap<>();
    storage.forEach((gameWeaponType, weaponAmmo)
        -> currentAmmo.put(gameWeaponType, weaponAmmo.getCurrentAmmo()));
    return currentAmmo;
  }
}
