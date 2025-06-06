package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class ShotgunDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .defaultDamage(gameReader.getGameConfig().getDefaultShotgunDamage())
        .maxDistance(7.0)
        .attackDelayMls(gameReader.getGameConfig().getShotgunDelayMls())
        .maxAmmo(gameReader.getGameConfig().getShotgunMaxAmmo())
        .distanceDamageAmplifier(
            distance -> {
              if (distance < 1) {
                return 3.0;
              } else if (distance < 2) {
                return 2.0;
              }
              return 1.0;
            }).build();
  }
}
