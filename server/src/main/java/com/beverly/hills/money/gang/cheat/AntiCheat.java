package com.beverly.hills.money.gang.cheat;

import com.beverly.hills.money.gang.config.GameRoomServerConfig;
import com.beverly.hills.money.gang.factory.rpg.RPGStatsFactory;
import com.beverly.hills.money.gang.state.Damage;
import com.beverly.hills.money.gang.state.PlayerRPGStatType;
import com.beverly.hills.money.gang.state.entity.Box;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import com.beverly.hills.money.gang.state.entity.Vector;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// TODO check attack rate
// TODO check that attacker is looking at victim
@Component
public class AntiCheat {

  private static final Logger LOG = LoggerFactory.getLogger(AntiCheat.class);

  private static final float PLAYER_SIZE = 0.2f;

  private static final float TOO_CLOSE_SHOOTING_DISTANCE = 1f;

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

  public boolean isCrossingWalls(final Vector shooterPosition, final Vector victimPosition,
      final List<Box> allWalls) {
    var distance = Vector.getDistance(shooterPosition, victimPosition);
    if (distance < TOO_CLOSE_SHOOTING_DISTANCE) {
      // standing too close. no wall check
      return false;
    }

    // we check 4 points around the player
    // if all points are covered by a wall then the player is totally behind the wall
    // we have to do this, because the center point might be behind the wall but
    // the player's ass might be still visible and shootable
    var victimPositions = List.of(
        victimPosition.add(PLAYER_SIZE, 0),
        victimPosition.add(0, PLAYER_SIZE),
        victimPosition.add(-PLAYER_SIZE, 0),
        victimPosition.add(0, -PLAYER_SIZE));

    return allWalls.stream().anyMatch(wall -> victimPositions.stream().allMatch(
        vector -> wall.isCrossing(shooterPosition, vector)));
  }

}
