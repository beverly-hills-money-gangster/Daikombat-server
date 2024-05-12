package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.powerup.PowerUp;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerPowerUpGameState {

  private final PlayerState playerState;

  private final PowerUp powerUp;

}
