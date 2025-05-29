package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class PlasmaGunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder().maxDistance(999)
        .attackDelayMls(gameReader.getGameConfig().getPlasmagunDelayMls())
        .distanceDamageAmplifier(distance -> 0.0)
        .maxAmmo(gameReader.getGameConfig().getPlasmagunMaxAmmo())
        .build();
  }
}
