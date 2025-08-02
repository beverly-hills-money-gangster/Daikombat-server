package com.beverly.hills.money.gang.state;

import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class Damage {

  private final Integer defaultDamage;

  private final double maxDistance;

  private final Integer attackDelayMls;

  private final Function<Double, Double> distanceDamageAmplifier;

  private final Integer maxAmmo;

  private final boolean selfInflicting;
}
