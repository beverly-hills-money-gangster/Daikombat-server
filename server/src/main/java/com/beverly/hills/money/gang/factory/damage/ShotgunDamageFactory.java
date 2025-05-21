package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class ShotgunDamageFactory extends DamageFactory {

  private final Damage damage = new Damage(ServerConfig.DEFAULT_SHOTGUN_DAMAGE, 7.0,
      ServerConfig.SHOTGUN_DELAY_MLS,
      distance -> {
        if (distance < 1) {
          return 3.0;
        } else if (distance < 2) {
          return 2.0;
        }
        return 1.0;
      });

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return damage;
  }
}
