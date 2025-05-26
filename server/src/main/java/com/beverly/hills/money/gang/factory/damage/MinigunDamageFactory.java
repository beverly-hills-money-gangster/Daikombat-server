package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class MinigunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(gameReader.getGameConfig().getDefaultMinigunDamage(), 7.0,
        gameReader.getGameConfig().getMinigunDelayMls(),
        distance -> 1.0);
  }
}
