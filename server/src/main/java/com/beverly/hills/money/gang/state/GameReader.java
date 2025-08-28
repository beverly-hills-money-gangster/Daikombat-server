package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.config.GameRoomServerConfig;
import com.beverly.hills.money.gang.spawner.map.GameMapMetadata;
import com.beverly.hills.money.gang.state.entity.RPGWeaponInfo;

public interface GameReader {

  int gameId();

  int getMatchId();

  int playersOnline();

  int maxPlayersAvailable();

  RPGWeaponInfo getRpgWeaponInfo();

  GameRoomServerConfig getGameConfig();

  GameMapMetadata getGameMapMetadata();
}
