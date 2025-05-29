package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// TODO cover with integration tests
public class BigAmmoPowerUp implements PowerUp {

  private static final float BIG_AMMO_AMPLIFIER = 1;

  private final Spawner spawner;

  @Override
  public PowerUpType getType() {
    return PowerUpType.BIG_AMMO;
  }

  @Override
  public Vector getSpawnPosition() {
    return spawner.spawnBigAmmo();
  }

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
