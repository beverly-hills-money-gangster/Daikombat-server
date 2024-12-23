package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.entity.Vector;
import org.springframework.stereotype.Component;

// TODO check attack rate
// TODO check that attacker is looking at victim
// TODO check that no player is going through walls
@Component
public class AntiCheat {

  private static final double MAX_POWER_UP_DISTANCE = 1.5;

  private static final double MAX_TELEPORT_DISTANCE = 1.5;

  // 20% error
  private static final double MAX_DISTANCE_TRAVELLED_IN_ONE_SEC = ServerConfig.PLAYER_SPEED * 1.2;

  public boolean isAttackingTooFar(final Vector position, final Vector victimPosition,
      final GameWeaponType gameWeaponType) {
    return Vector.getDistance(position, victimPosition) > gameWeaponType.getMaxDistance();
  }

  public boolean isPowerUpTooFar(final Vector playerPosition, final Vector powerUpPosition) {
    return Vector.getDistance(playerPosition, powerUpPosition) > MAX_POWER_UP_DISTANCE;
  }

  public boolean isTeleportTooFar(final Vector playerPosition, final Vector teleportPosition) {
    return Vector.getDistance(playerPosition, teleportPosition) > MAX_TELEPORT_DISTANCE;
  }

  public boolean isTooMuchDistanceTravelled(
      final double distanceTravelled, final int periodSec) {
    return distanceTravelled > MAX_DISTANCE_TRAVELLED_IN_ONE_SEC * periodSec;
  }

}
