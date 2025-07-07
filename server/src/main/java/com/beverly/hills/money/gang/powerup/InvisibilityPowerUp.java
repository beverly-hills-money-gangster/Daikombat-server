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
public class InvisibilityPowerUp implements PowerUp {


  @Getter
  private final PowerUpType type = PowerUpType.INVISIBILITY;

  @Getter
  private final Vector position;

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
