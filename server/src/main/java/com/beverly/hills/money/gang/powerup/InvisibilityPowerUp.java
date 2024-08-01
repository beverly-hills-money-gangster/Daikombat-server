package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvisibilityPowerUp implements PowerUp {

  private final Spawner spawner;

  @Override
  public PowerUpType getType() {
    return PowerUpType.INVISIBILITY;
  }

  @Override
  public Vector getSpawnPosition() {
    return spawner.spawnInvisibility();
  }

  @Override
  public void apply(PlayerState playerState) {
    // player state doesn't change
  }

  @Override
  public void revert(PlayerState playerState) {
    // player state doesn't change
  }

  @Override
  public int getSpawnPeriodMls() {
    return ServerConfig.INVISIBILITY_SPAWN_MLS;
  }

  @Override
  public int getLastsForMls() {
    return ServerConfig.INVISIBILITY_LASTS_FOR_MLS;
  }


}
