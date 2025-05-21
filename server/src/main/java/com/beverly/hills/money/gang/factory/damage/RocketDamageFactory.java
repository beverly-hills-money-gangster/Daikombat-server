package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RocketDamageFactory extends DamageFactory {

  private final Damage damage = new Damage(ServerConfig.DEFAULT_ROCKET_DAMAGE, 1.75f, 0,
      distance -> {
        if (distance <= 1) {
          return 1.0;
        }
        return 1.0 / distance;
      });

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return damage;
  }
}
