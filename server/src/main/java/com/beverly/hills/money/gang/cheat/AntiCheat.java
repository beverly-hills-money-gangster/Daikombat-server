package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.factory.RPGStatsFactory;
import com.beverly.hills.money.gang.state.GameWeaponType;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.Vector;
import org.springframework.stereotype.Component;

// TODO check attack rate
// TODO check that attacker is looking at victim
// TODO check that no player is going through walls
@Component
public class AntiCheat {

  private static final double MAX_POWER_UP_DISTANCE = 2;

  private static final double MAX_TELEPORT_DISTANCE = 2;

  public static float getMaxSpeed(final RPGPlayerClass playerClass) {
    return (float) (ServerConfig.PLAYER_SPEED * RPGStatsFactory.create(playerClass).getNormalized(
        PlayerRPGStatType.RUN_SPEED));
  }

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

}
