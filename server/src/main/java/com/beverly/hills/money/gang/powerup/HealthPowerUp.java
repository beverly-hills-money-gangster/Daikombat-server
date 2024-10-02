package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HealthPowerUp implements PowerUp {

  private final Spawner spawner;

  @Override
  public PowerUpType getType() {
    return PowerUpType.HEALTH;
  }

  @Override
  public Vector getSpawnPosition() {
    return spawner.spawnHealth();
  }

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
    // health restore is instant. this value is just for the visual effects
    return 500;
  }

}
