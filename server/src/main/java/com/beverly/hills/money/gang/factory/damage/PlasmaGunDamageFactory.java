package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PlasmaGunDamageFactory implements DamageFactory {

  private final Damage damage = new Damage(0, 999,
      ServerConfig.PLASMAGUN_DELAY_MLS, distance -> 0.0);

  @Override
  public Damage getDamage(GameReader gameReader) {
    return damage;
  }
}
