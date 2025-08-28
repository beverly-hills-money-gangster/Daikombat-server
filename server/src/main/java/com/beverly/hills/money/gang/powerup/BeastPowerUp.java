package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BeastPowerUp implements PowerUp {

  @Getter
  private final PowerUpType type = PowerUpType.BEAST;

  private static final int DEFENCE_AMPLIFIER = 2;

  private static final int DAMAGE_AMPLIFIER = 2;

  @Getter
  private final Vector position;


  @Override
  public void apply(PlayerState playerState) {
    playerState.amplifyDamage(DAMAGE_AMPLIFIER);
    playerState.amplifyDefence(DEFENCE_AMPLIFIER);
    playerState.restoreHealth();
    playerState.restoreAllAmmo(1);
  }

  @Override
  public void revert(PlayerState playerState) {
    playerState.amplifyDamage(1D / DAMAGE_AMPLIFIER);
    playerState.amplifyDefence(1D / DEFENCE_AMPLIFIER);

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
