package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class MinigunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .defaultDamage(gameReader.getGameConfig().getDefaultMinigunDamage())
        .maxDistance(7.0)
        .attackDelayMls(gameReader.getGameConfig().getMinigunDelayMls())
        .distanceDamageAmplifier(distance -> 1.0)
        .maxAmmo(gameReader.getGameConfig().getMinigunMaxAmmo())
        .build();
  }
}
