package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RailgunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .defaultDamage(gameReader.getGameConfig().getDefaultRailgunDamage())
        .maxDistance(10.0)
        .attackDelayMls(gameReader.getGameConfig().getRailgunDelayMls())
        .distanceDamageAmplifier(distance -> 1.0)
        .maxAmmo(gameReader.getGameConfig().getRailgunMaxAmmo())
        .build();
  }
}
