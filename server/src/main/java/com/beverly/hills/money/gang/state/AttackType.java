package com.beverly.hills.money.gang.state;

import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AttackType {
  // shotgun damage increases as the player gets closer to the victim
  SHOTGUN(distance -> {
    if (distance < 1) {
      return 3;
    } else if (distance < 2) {
      return 2;
    }
    return 1;
  }),
  PUNCH(distance -> 1),
  RAILGUN(distance -> 1),
  MINIGUN(distance -> 1);

  @Getter
  private final Function<Double, Integer> distanceDamageAmplifier;
}
