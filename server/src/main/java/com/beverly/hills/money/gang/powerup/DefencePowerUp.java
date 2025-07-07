package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;



@RequiredArgsConstructor
public class DefencePowerUp implements PowerUp {

  private static final int DEFENCE_AMPLIFIER = 2;

  @Getter
  private final Vector position;

  @Getter
  private final PowerUpType type = PowerUpType.DEFENCE;

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
