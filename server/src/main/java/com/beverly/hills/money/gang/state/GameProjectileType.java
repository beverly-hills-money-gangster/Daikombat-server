package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameProjectileType implements Damage {
  ROCKET(ServerConfig.DEFAULT_ROCKET_DAMAGE, 1.5f, 0,
      distance -> {
        if (distance <= 1) {
          return 1.0;
        }
        return 1.0 / distance;
      }),
  PLASMA(ServerConfig.DEFAULT_PLASMA_DAMAGE, 0.5f, 0, distance -> 1.0);

  @Getter
  private final int defaultDamage;

  @Getter
  private final double maxDistance;

  @Getter
  private final int attackDelayMls;

  @Getter
  private final Function<Double, Double> distanceDamageAmplifier;

}
