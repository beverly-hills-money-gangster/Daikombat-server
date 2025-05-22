package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PunchDamageFactory extends DamageFactory {


  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(gameReader.getGameConfig().getDefaultPunchDamage(),
        1.2, gameReader.getGameConfig().getPunchDelayMls(),
        distance -> 1.0);
  }
}
