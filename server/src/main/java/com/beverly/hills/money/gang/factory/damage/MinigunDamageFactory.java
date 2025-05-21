package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class MinigunDamageFactory extends DamageFactory {

  private final Damage damage = new Damage(ServerConfig.DEFAULT_MINIGUN_DAMAGE, 7.0,
      ServerConfig.MINIGUN_DELAY_MLS,
      distance -> 1.0);

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return damage;
  }
}
