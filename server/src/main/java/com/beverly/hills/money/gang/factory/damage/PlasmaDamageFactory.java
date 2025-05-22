package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PlasmaDamageFactory extends DamageFactory {


  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(gameReader.getGameConfig().getDefaultPlasmaDamage(),
        1f, 0, distance -> 1.0);
  }
}
