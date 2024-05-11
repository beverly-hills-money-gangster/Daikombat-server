package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuadDamagePowerUp implements PowerUp {

  private final Spawner spawner;

  @Override
  public PowerUpType getType() {
    return PowerUpType.QUAD_DAMAGE;
  }

  @Override
  public Vector getSpawnPosition() {
    return spawner.spawnQuadDamage();
  }

  @Override
  public void apply(PlayerState playerState) {
    playerState.quadDamage();
  }

  @Override
  public void revert(PlayerState playerState) {
    playerState.defaultDamage();
  }

  @Override
  public int getSpawnPeriodMls() {
    return ServerConfig.QUAD_DAMAGE_SPAWN_MLS;
  }

  @Override
  public int getLastsForMls() {
    return ServerConfig.QUAD_DAMAGE_LASTS_FOR_MLS;
  }


}
