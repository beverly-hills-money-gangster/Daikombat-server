package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.config.GameRoomServerConfig;
import com.beverly.hills.money.gang.factory.rpg.RPGStatsFactory;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.state.entity.Wall;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// TODO check attack rate
// TODO check that attacker is looking at victim
// TODO check that no player is going through walls
@Component
public class AntiCheat {

  private static final Logger LOG = LoggerFactory.getLogger(AntiCheat.class);

  private static final double MAX_POWER_UP_DISTANCE = 2;

  private static final double MAX_TELEPORT_DISTANCE = 2;

  public static float getMaxSpeed(final RPGPlayerClass playerClass,
      final GameRoomServerConfig gameRoomServerConfig) {
    return (float) (gameRoomServerConfig.getPlayerSpeed() * RPGStatsFactory.create(playerClass)
        .getNormalized(
            PlayerRPGStatType.RUN_SPEED));
  }

  public boolean isAttackingTooFar(final Vector position, final Vector victimPosition,
      final Damage damage) {
    return Vector.getDistance(position, victimPosition) > damage.getMaxDistance();
  }

  public boolean isPowerUpTooFar(final Vector playerPosition, final Vector powerUpPosition) {
    return Vector.getDistance(playerPosition, powerUpPosition) > MAX_POWER_UP_DISTANCE;
  }

  public boolean isTeleportTooFar(final Vector playerPosition, final Vector teleportPosition) {
    return Vector.getDistance(playerPosition, teleportPosition) > MAX_TELEPORT_DISTANCE;
  }

  // TODO move to separate class
  List<Wall> getAllWallsInProximity(
      final Vector playerPosition, final double radius, final List<Wall> allWalls) {
    if (radius < 0) {
      throw new IllegalArgumentException("Radius can't be negative");
    } else if (radius == 0) {
      return List.of();
    } else if (allWalls.isEmpty()) {
      return List.of();
    }
    // TODO implement
    return null;
  }

  public boolean isCrossingWalls(final Vector playerPosition, final Vector victimPosition,
      final List<Wall> allWalls) {
    // TODO add performance test
    var distance = Vector.getDistance(playerPosition, victimPosition);
    if (distance < 1.5f) { // TODO create const for that
      // standing too close. no wall check
      return false;
    }
    // TODO get close walls only
    return allWalls.stream()
        .anyMatch(wall -> {
          if (wall.isCrossing(playerPosition, victimPosition)) {
            LOG.warn("Crossed wall {}", wall.getId());
            return true;
          }
          return false;

        });
  }

}
