package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

// TODO make sure Beast can't be stacked with other powerups
@RequiredArgsConstructor
public class BeastPowerUp implements PowerUp {

  @Getter
  private final PowerUpType type = PowerUpType.BEAST;

  private static final int DEFENCE_AMPLIFIER = 2;

  @Getter
  private final Vector position;

  @Override
  public void apply(PlayerState playerState) {
    playerState.quadDamage();
    playerState.setDefenceAmplifier(DEFENCE_AMPLIFIER);
    playerState.restoreHealth();
    playerState.restoreAllAmmo(1);
  }

  @Override
  public void revert(PlayerState playerState) {
    playerState.defaultDamage();
    playerState.defaultDefence();

  }

  @Override
  public int getSpawnPeriodMls() {
    return ServerConfig.BEAST_SPAWN_MLS;
  }

  @Override
  public int getLastsForMls() {
    return ServerConfig.BEAST_LASTS_FOR_MLS;
  }


}
