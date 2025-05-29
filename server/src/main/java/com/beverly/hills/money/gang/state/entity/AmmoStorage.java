package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.GameReader;
import com.beverly.hills.money.gang.state.GameWeaponType;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


public class AmmoStorage implements AmmoStorageReader {

  private final Map<GameWeaponType, WeaponAmmo> storage = new ConcurrentHashMap<>();

  private final GameReader gameReader;

  public AmmoStorage(final GameReader gameReader) {
    this.gameReader = gameReader;
    for (GameWeaponType weaponType : GameWeaponType.values()) {
      Optional.ofNullable(weaponType.getDamageFactory().getDamage(gameReader).getMaxAmmo())
          .ifPresent(
              maxAmmo -> storage.put(weaponType, new WeaponAmmo(maxAmmo)));
    }
  }

  public void restore(final GameWeaponType weaponType, final float ratio) {
    int currentAmmo = storage.get(weaponType).getCurrentAmmo();
    Optional.ofNullable(weaponType.getDamageFactory().getDamage(gameReader).getMaxAmmo()).ifPresent(
        maxAmmo -> storage.put(weaponType,
            new WeaponAmmo(Math.min(maxAmmo, currentAmmo + (int) (maxAmmo * Math.abs(ratio))))));

  }

  public boolean wasteAmmo(final GameWeaponType weaponType) {
    return Optional.ofNullable(storage.get(weaponType))
        .map(WeaponAmmo::wasteAmmo)
        // if ammo is not specified(like melee), then we let it go
        .orElse(true);
  }


  @Override
  public Map<GameWeaponType, Integer> getCurrentAmmo() {
    Map<GameWeaponType, Integer> currentAmmo = new HashMap<>();
    storage.forEach((gameWeaponType, weaponAmmo)
        -> currentAmmo.put(gameWeaponType, weaponAmmo.getCurrentAmmo()));
    return currentAmmo;
  }
}
