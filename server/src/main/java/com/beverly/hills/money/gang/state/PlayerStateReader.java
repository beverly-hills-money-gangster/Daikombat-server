package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.state.entity.AmmoStorageReader;
import com.beverly.hills.money.gang.state.entity.PlayerActivityStatus;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerState.PowerUpInEffect;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.RPGPlayerClass;
import java.util.List;

public interface PlayerStateReader {

  List<PowerUpInEffect> getActivePowerUps();

  int getMatchId();

  Coordinates getCoordinates();

  PlayerStateColor getColor();

  PlayerActivityStatus getActivityStatus();

  int getNextEventId();

  int getLastReceivedEventSequenceId();

  int getPingMls();

  int getPlayerId();

  String getPlayerName();

  int getHealth();

  boolean isDead();

  boolean hasMoved();

  PlayerGameStatsReader getGameStats();

  int getGameSession();

  RPGPlayerClass getRpgPlayerClass();

  float getSpeed();

  AmmoStorageReader getAmmoStorageReader();

}
