package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MediumAmmoPowerUp implements PowerUp {

  private static final float MEDIUM_AMMO_AMPLIFIER = 0.5f;

  @Getter
  private final PowerUpType type = PowerUpType.MEDIUM_AMMO;

  @Getter
  private final Vector position;

  @Override
  public void apply(PlayerState playerState) {
    playerState.restoreAllAmmo(MEDIUM_AMMO_AMPLIFIER);
  }

  @Override
  public void revert(PlayerState playerState) {

  }

  @Override
  public int getSpawnPeriodMls() {
    return ServerConfig.AMMO_SPAWN_MLS;
  }

  @Override
  public int getLastsForMls() {
    // ammo restore is instant
    return 0;
  }

}
