package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RocketLauncherDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(0, 999,
        gameReader.getGameConfig().getRocketLauncherDelayMls(),
        distance -> 0.0);
  }
}
