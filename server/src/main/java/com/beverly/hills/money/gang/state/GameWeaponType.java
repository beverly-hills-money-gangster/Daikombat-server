package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.factory.damage.DamageFactory;
import com.beverly.hills.money.gang.factory.damage.MinigunDamageFactory;
import com.beverly.hills.money.gang.factory.damage.PlasmaGunDamageFactory;
import com.beverly.hills.money.gang.factory.damage.PunchDamageFactory;
import com.beverly.hills.money.gang.factory.damage.RailgunDamageFactory;
import com.beverly.hills.money.gang.factory.damage.RocketLauncherDamageFactory;
import com.beverly.hills.money.gang.factory.damage.ShotgunDamageFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameWeaponType {
  SHOTGUN(new ShotgunDamageFactory()),
  PUNCH(new PunchDamageFactory()),
  RAILGUN(new RailgunDamageFactory()),
  MINIGUN(new MinigunDamageFactory()),

  ROCKET_LAUNCHER(new RocketLauncherDamageFactory()),
  PLASMAGUN(new PlasmaGunDamageFactory());

  @Getter
  private final DamageFactory damageFactory;


}
