package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class EnergyDamageFactory extends DamageFactory {


  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .defaultDamage(gameReader.getGameConfig().getDefaultPunchDamage())
        .maxDistance(0.5f)
        .distanceDamageAmplifier(distance -> 1.0).build();
  }
}
