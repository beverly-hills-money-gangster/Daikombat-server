package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.powerup.PowerUp;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerPowerUpGameState {

  private final PlayerState playerState;

  private final PowerUp powerUp;

}
