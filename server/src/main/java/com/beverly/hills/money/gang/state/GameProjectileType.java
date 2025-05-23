package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.factory.damage.DamageFactory;
import com.beverly.hills.money.gang.factory.damage.PlasmaDamageFactory;
import com.beverly.hills.money.gang.factory.damage.RocketDamageFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameProjectileType {
  ROCKET(new RocketDamageFactory()),
  PLASMA(new PlasmaDamageFactory());

  @Getter
  private final DamageFactory damageFactory;

}
