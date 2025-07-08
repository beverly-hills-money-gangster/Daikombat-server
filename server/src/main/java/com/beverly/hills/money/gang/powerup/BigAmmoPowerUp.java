package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BigAmmoPowerUp implements PowerUp {

  private static final float BIG_AMMO_AMPLIFIER = 1;

  @Getter
  private final Vector position;

  @Getter
  private final PowerUpType type = PowerUpType.BIG_AMMO;

  @Override
  public void apply(PlayerState playerState) {
    playerState.restoreAllAmmo(BIG_AMMO_AMPLIFIER);
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
