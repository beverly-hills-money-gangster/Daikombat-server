package com.beverly.hills.money.gang.state;

import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AttackType {
  // shotgun damage increases as the player gets closer to the victim
  SHOTGUN(distance -> {
    if (distance < 1) {
      return 3.0;
    } else if (distance < 2) {
      return 2.0;
    }
    return 1.0;
  }, false),
  PUNCH(distance -> 1.0, false),
  RAILGUN(distance -> 1.0, false),
  MINIGUN(distance -> 1.0, false),

  ROCKET(distance -> {
    if (distance < 1) {
      return 1.0;
    }
    return 1 / distance;
  }, true),

  // rocket launcher itself doesn't do any damage
  ROCKET_LAUNCHER(distance -> 0.0, false);

  @Getter
  private final Function<Double, Double> distanceDamageAmplifier;

  @Getter
  private final boolean projectile;

}
