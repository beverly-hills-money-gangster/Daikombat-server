package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuadDamagePowerUp implements PowerUp {

  @Getter
  private final PowerUpType type = PowerUpType.QUAD_DAMAGE;

  @Getter
  private final Vector position;

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
