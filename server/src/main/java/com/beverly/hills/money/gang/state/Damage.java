package com.beverly.hills.money.gang.state;

import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Damage {

  private final int defaultDamage;

  private final double maxDistance;

  private final int attackDelayMls;

  private final Function<Double, Double> distanceDamageAmplifier;
}
