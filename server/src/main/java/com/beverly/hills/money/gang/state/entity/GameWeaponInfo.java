package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.GameWeaponType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameWeaponInfo {

  private final GameWeaponType gameWeaponType;

  private final double maxDistance;

  private final Integer delayMls;

  private final Integer maxAmmo;

}
