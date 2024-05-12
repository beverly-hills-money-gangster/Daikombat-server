package com.beverly.hills.money.gang.powerup;

import com.beverly.hills.money.gang.state.PlayerState;
import com.beverly.hills.money.gang.state.Vector;

public interface PowerUp {

  PowerUpType getType();

  Vector getSpawnPosition();

  void apply(PlayerState playerState);

  void revert(PlayerState playerState);

  int getSpawnPeriodMls();

  int getLastsForMls();

}
