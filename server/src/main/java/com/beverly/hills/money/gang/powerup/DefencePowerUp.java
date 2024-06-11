package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefencePowerUp implements PowerUp {

  private static final int DEFENCE_AMPLIFIER = 2;

  private final Spawner spawner;

  @Override
  public PowerUpType getType() {
    return PowerUpType.DEFENCE;
  }

  @Override
  public Vector getSpawnPosition() {
    return spawner.spawnDefence();
  }

  @Override
  public void apply(PlayerState playerState) {
    playerState.setDefenceAmplifier(DEFENCE_AMPLIFIER);
  }

  @Override
  public void revert(PlayerState playerState) {
    playerState.defaultDefence();
  }

  @Override
  public int getSpawnPeriodMls() {
    return ServerConfig.DEFENCE_SPAWN_MLS;
  }

  @Override
  public int getLastsForMls() {
    return ServerConfig.DEFENCE_LASTS_FOR_MLS;
  }

}
