package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;

public interface PowerUp {

  Vector getPosition();

  PowerUpType getType();

  void apply(PlayerState playerState);

  void revert(PlayerState playerState);

  int getSpawnPeriodMls();

  int getLastsForMls();

}
