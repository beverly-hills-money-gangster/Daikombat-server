package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.state.entity.PlayerState;
import com.beverly.hills.money.gang.state.entity.PlayerState.PowerUpInEffect;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import java.util.List;
import javax.annotation.Nullable;

public interface PlayerStateReader {

  List<PowerUpInEffect> getActivePowerUps();

  PlayerState.PlayerCoordinates getCoordinates();

  PlayerStateColor getColor();

  boolean isFullyJoined();

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
