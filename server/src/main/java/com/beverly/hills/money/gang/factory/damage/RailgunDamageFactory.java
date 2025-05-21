package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RailgunDamageFactory implements DamageFactory {

  private final Damage damage = new Damage(ServerConfig.DEFAULT_RAILGUN_DAMAGE, 10.0,
      ServerConfig.RAILGUN_DELAY_MLS,
      distance -> 1.0);

  @Override
  public Damage getDamage(GameReader gameReader) {
    return damage;
  }
}
