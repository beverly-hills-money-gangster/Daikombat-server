package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.state.PlayerState.PowerUpInEffect;
import java.util.List;
import java.util.stream.Stream;

public interface PlayerStateReader {

  List<PowerUpInEffect> getActivePowerUps();

  PlayerState.PlayerCoordinates getCoordinates();

  PlayerStateColor getColor();

  int getPlayerId();

  String getPlayerName();

  int getHealth();

  boolean isDead();

  boolean hasMoved();


}
