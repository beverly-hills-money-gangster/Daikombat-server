package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RocketLauncherDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .defaultDamage(0)
        .maxDistance(999)
        .attackDelayMls(gameReader.getGameConfig().getRocketLauncherDelayMls())
        .maxAmmo(gameReader.getGameConfig().getRocketLauncherMaxAmmo())
        .distanceDamageAmplifier(distance -> 0.0).build();
  }
}
