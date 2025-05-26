package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RocketDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(gameReader.getGameConfig().getDefaultRocketDamage(), 1.75f, 0,
        distance -> {
          if (distance <= 1) {
            return 1.0;
          }
          return 1.0 / distance;
        });
  }
}
