package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.GameRoomServerConfig;
import com.beverly.hills.money.gang.state.entity.RPGWeaponInfo;

public interface GameReader {

  int gameId();

  int matchId();

  int playersOnline();

  int maxPlayersAvailable();

  RPGWeaponInfo getRpgWeaponInfo();

  GameRoomServerConfig getGameConfig();
}
