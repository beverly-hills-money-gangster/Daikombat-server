package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.ServerConfig;
import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameWeaponType implements Damage {
  // shotgun damage increases as the player gets closer to the victim
  SHOTGUN(ServerConfig.DEFAULT_SHOTGUN_DAMAGE, 7.0, ServerConfig.SHOTGUN_DELAY_MLS,
      distance -> {
        if (distance < 1) {
          return 3.0;
        } else if (distance < 2) {
          return 2.0;
        }
        return 1.0;
      }),
  PUNCH(ServerConfig.DEFAULT_PUNCH_DAMAGE, 1.2, ServerConfig.PUNCH_DELAY_MLS, distance -> 1.0),
  RAILGUN(ServerConfig.DEFAULT_RAILGUN_DAMAGE, 10.0, ServerConfig.RAILGUN_DELAY_MLS,
      distance -> 1.0),
  MINIGUN(ServerConfig.DEFAULT_MINIGUN_DAMAGE, 7.0, ServerConfig.MINIGUN_DELAY_MLS,
      distance -> 1.0),

  // rocket launcher itself doesn't do any damage
  ROCKET_LAUNCHER(0, 999, ServerConfig.ROCKET_LAUNCHER_DELAY_MLS, istance -> 0.0);

  @Getter
  private final int defaultDamage;

  @Getter
  private final double maxDistance;

  @Getter
  private final int attackDelayMls;

  @Getter
  private final Function<Double, Double> distanceDamageAmplifier;


}
