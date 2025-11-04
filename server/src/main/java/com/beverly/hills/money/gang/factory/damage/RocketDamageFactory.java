package com.beverly.hills.money.gang.factory.damage;

import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.GameReader;

public class RocketDamageFactory extends DamageFactory {

  @Override
  protected Damage createDamage(GameReader gameReader) {
    return Damage.builder()
        .defaultDamage(gameReader.getGameConfig().getDefaultRocketDamage())
        .maxDistance(1.25f)
        .distanceDamageAmplifier(distance -> {
          if (distance <= 1) {
            return 1.0;
          }
          return 1.0 / distance;
        }).build();
  }
}
