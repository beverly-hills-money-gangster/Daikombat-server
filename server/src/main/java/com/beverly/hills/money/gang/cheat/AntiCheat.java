package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.AttackType;
import com.beverly.hills.money.gang.state.entity.AttackInfo;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

// TODO check attack rate
// TODO check that attacker is looking at victim
// TODO check that no player is going through walls
@Component
public class AntiCheat {


  private static final Map<AttackType, Double> MAX_ATTACK_DISTANCE = Map.of(
      AttackType.PUNCH, 1.2,
      AttackType.SHOTGUN, 5.5,
      AttackType.RAILGUN, 7.5);

  private static final Map<AttackType, Integer> ATTACK_DELAY_MLS = Map.of(
      AttackType.PUNCH, 300,
      AttackType.SHOTGUN, 450,
      AttackType.RAILGUN, 1_500);

  public static final List<AttackInfo> ATTACKS_INFO;

  static {
    if (MAX_ATTACK_DISTANCE.size() != AttackType.values().length) {
      throw new IllegalStateException("Not all attack types have max distance");
    }
    if (ATTACK_DELAY_MLS.size() != AttackType.values().length) {
      throw new IllegalStateException("Not all attack types have delay");
    }
    ATTACKS_INFO = Arrays.stream(AttackType.values()).map(
            attackType -> AttackInfo
                .builder()
                .attackType(attackType)
                .delayMls(ATTACK_DELAY_MLS.get(attackType))
                .maxDistance(MAX_ATTACK_DISTANCE.get(attackType))
                .build())
        .collect(Collectors.toList());
  }

  private static final double MAX_POWER_UP_DISTANCE = 1.5;

  private static final double MAX_TELEPORT_DISTANCE = 1.5;

  // 20% error
  private static final double MAX_DISTANCE_TRAVELLED_IN_ONE_SEC = ServerConfig.PLAYER_SPEED * 1.2;

  public boolean isAttackingTooFar(final Vector shooterPosition, final Vector victimPosition,
      final AttackType attackType) {
    return Vector.getDistance(shooterPosition, victimPosition) > MAX_ATTACK_DISTANCE.get(
        attackType);
  }

  public boolean isPowerUpTooFar(final Vector playerPosition, final Vector powerUpPosition) {
    return Vector.getDistance(playerPosition, powerUpPosition) > MAX_POWER_UP_DISTANCE;
  }

  public boolean isTeleportTooFar(final Vector playerPosition, final Vector teleportPosition) {
    return Vector.getDistance(playerPosition, teleportPosition) > MAX_TELEPORT_DISTANCE;
  }

  public boolean isTooMuchDistanceTravelled(final double distanceTravelled,
      final int periodSec) {
    return distanceTravelled > MAX_DISTANCE_TRAVELLED_IN_ONE_SEC * periodSec;
  }

}
