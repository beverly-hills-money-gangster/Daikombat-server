package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class DefencePowerUp implements PowerUp {

  private static final int DEFENCE_AMPLIFIER = 2;

  @Getter
  private final Vector position;

  @Getter
  private final PowerUpType type = PowerUpType.DEFENCE;

  @Override
  public void apply(@NonNull PlayerState playerState) {
    playerState.amplifyDefence(DEFENCE_AMPLIFIER);
  }

  @Override
  public void revert(PlayerState playerState) {
    playerState.amplifyDefence(1D / DEFENCE_AMPLIFIER);
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
