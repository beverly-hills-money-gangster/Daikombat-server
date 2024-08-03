package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.Vector;

public interface PowerUp {

  PowerUpType getType();

  Vector getSpawnPosition();

  void apply(PlayerState playerState);

  void revert(PlayerState playerState);

  int getSpawnPeriodMls();

  int getLastsForMls();

}
