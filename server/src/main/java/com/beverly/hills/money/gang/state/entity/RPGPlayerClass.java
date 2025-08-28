package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.GameWeaponType;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RPGPlayerClass {
  WARRIOR(Set.of(GameWeaponType.values())),
  ANGRY_SKELETON(Set.of(GameWeaponType.PUNCH, GameWeaponType.RAILGUN, GameWeaponType.PLASMAGUN)),
  DEMON_TANK(Set.of(GameWeaponType.PUNCH, GameWeaponType.MINIGUN, GameWeaponType.ROCKET_LAUNCHER));

  @Getter
  private final Set<GameWeaponType> weapons;

}
