package com.beverly.hills.money.gang.state;

import java.util.function.Function;

public interface Damage {

  double getMaxDistance();

  int getDefaultDamage();

  int getAttackDelayMls();

  Function<Double, Double> getDistanceDamageAmplifier();
}
