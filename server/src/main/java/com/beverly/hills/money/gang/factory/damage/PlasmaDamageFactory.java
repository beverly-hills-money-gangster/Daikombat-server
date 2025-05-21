package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PlasmaDamageFactory implements DamageFactory {

  private final Damage damage = new Damage(ServerConfig.DEFAULT_PLASMA_DAMAGE, 1f, 0,
      distance -> 1.0);

  @Override
  public Damage getDamage(GameReader gameReader) {
    return damage;
  }
}
