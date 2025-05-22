package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PlasmaGunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return new Damage(0, 999,
        gameReader.getGameConfig().getPlasmagunDelayMls(), distance -> 0.0);
  }
}
