package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PunchDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .maxDistance(1.0)
        .attackDelayMls(gameReader.getGameConfig().getPunchDelayMls())
        .distanceDamageAmplifier(distance -> 0.0).build();
  }
}
