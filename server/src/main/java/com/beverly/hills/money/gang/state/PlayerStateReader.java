package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.state.PlayerState.PowerUpInEffect;
import java.util.List;
import javax.annotation.Nullable;

public interface PlayerStateReader {

  List<PowerUpInEffect> getActivePowerUps();

  PlayerState.PlayerCoordinates getCoordinates();

  PlayerStateColor getColor();

  int getNextEventId();

  int getLastReceivedEventSequenceId();

  @Nullable
  Integer getPingMls();

  int getPlayerId();

  String getPlayerName();

  int getHealth();

  boolean isDead();

  boolean hasMoved();


}
