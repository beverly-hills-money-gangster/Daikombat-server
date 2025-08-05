package com.beverly.hills.money.gang.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRPGStats {

  private final Map<PlayerRPGStatType, PlayerRPGStatValue> stats = new ConcurrentHashMap<>();

  public PlayerRPGStats(
      Map<PlayerRPGStatType, PlayerRPGStatValue> stats) {
    if (stats.size() != PlayerRPGStatType.values().length) {
      throw new IllegalArgumentException("Not all RPG stats are specified");
    }
    this.stats.putAll(stats);
  }


  public double getNormalized(final PlayerRPGStatType playerRPGStatType) {
    return stats.get(playerRPGStatType).getValue() / 100.0;
  }

}
