package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.factory.damage.DamageFactory;
import com.beverly.hills.money.gang.factory.damage.MinigunDamageFactory;
import com.beverly.hills.money.gang.factory.damage.PlasmaGunDamageFactory;
import com.beverly.hills.money.gang.factory.damage.PunchDamageFactory;
import com.beverly.hills.money.gang.factory.damage.RailgunDamageFactory;
import com.beverly.hills.money.gang.factory.damage.RocketLauncherDamageFactory;
import com.beverly.hills.money.gang.factory.damage.ShotgunDamageFactory;
import lombok.Getter;

public enum GameWeaponType {
  SHOTGUN(new ShotgunDamageFactory()),
  PUNCH(new PunchDamageFactory()),
  RAILGUN(new RailgunDamageFactory()),
  MINIGUN(new MinigunDamageFactory()),

  ROCKET_LAUNCHER(new RocketLauncherDamageFactory(), GameProjectileType.ROCKET),
  PLASMAGUN(new PlasmaGunDamageFactory(), GameProjectileType.PLASMA);

  @Getter
  private final DamageFactory damageFactory;

  @Getter
  private final GameProjectileType projectileType;

  GameWeaponType(DamageFactory damageFactory, GameProjectileType projectileType) {
    this.damageFactory = damageFactory;
    this.projectileType = projectileType;
  }

  GameWeaponType(DamageFactory damageFactory) {
    this.damageFactory = damageFactory;
    this.projectileType = null;
  }
}
