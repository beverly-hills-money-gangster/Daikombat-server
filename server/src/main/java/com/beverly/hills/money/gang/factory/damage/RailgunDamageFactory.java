package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RailgunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(gameReader.getGameConfig().getDefaultRailgunDamage(), 10.0,
        gameReader.getGameConfig().getRailgunDelayMls(),
        distance -> 1.0);
  }
}
