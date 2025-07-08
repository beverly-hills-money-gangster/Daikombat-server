package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class HealthPowerUp implements PowerUp {

  @Getter
  private final PowerUpType type = PowerUpType.HEALTH;

  @Getter
  private final Vector position;

  @Override
  public void apply(PlayerState playerState) {
    playerState.restoreHealth();
  }

  @Override
  public void revert(PlayerState playerState) {
    // do nothing
  }

  @Override
  public int getSpawnPeriodMls() {
    return ServerConfig.HEALTH_SPAWN_MLS;
  }

  @Override
  public int getLastsForMls() {
    // health restore is instant
    return 0;
  }

}
