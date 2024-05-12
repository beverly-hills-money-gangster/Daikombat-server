package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.state.PlayerState.PowerUpInEffect;
import java.util.stream.Stream;

public interface PlayerStateReader {

  Stream<PowerUpInEffect> getActivePowerUps();

  PlayerState.PlayerCoordinates getCoordinates();

  int getPlayerId();

  String getPlayerName();

  int getHealth();

  boolean isDead();

  boolean hasMoved();

}
