package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@EqualsAndHashCode
public class Vector {

  @Getter
  private final float x;
  @Getter
  private final float y;

  public static double getDistance(Vector v1, Vector v2) {
    float x = v1.getX() - v2.getX();
    float y = v1.getY() - v2.getY();
    return Math.sqrt(x * x + y * y);
  }
}
