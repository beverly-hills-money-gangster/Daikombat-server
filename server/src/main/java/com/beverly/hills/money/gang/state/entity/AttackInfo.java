package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.AttackType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AttackInfo {

  private final AttackType attackType;

  private final double maxDistance;

  private final int delayMls;

  private final int defaultDamage;

}
