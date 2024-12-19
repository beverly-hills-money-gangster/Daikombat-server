package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerState.PowerUpInEffect;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.util.List;

public interface PlayerStateReader {

  List<PowerUpInEffect> getActivePowerUps();

  Coordinates getCoordinates();

  PlayerStateColor getColor();

  boolean isFullyJoined();

  int getNextEventId();

  int getLastReceivedEventSequenceId();

  int getPingMls();

  int getPlayerId();

  String getPlayerName();

  int getHealth();

  boolean isDead();

  boolean hasMoved();

  PlayerGameStatsReader getGameStats();

  RPGPlayerClass getRpgPlayerClass();

}
