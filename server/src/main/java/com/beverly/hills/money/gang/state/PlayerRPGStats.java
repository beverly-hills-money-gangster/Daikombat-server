package com.beverly.hills.money.gang.state;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

public class PlayerRPGStats {

  private final Map<PlayerRPGStatType, PlayerRPGStatValue> stats = new ConcurrentHashMap<>();

  public PlayerRPGStats(
      Map<PlayerRPGStatType, PlayerRPGStatValue> stats) {
    if (stats.size() != PlayerRPGStatType.values().length) {
      throw new IllegalArgumentException("Not all RPG stats are specified");
    }
    this.stats.putAll(stats);
  }

  public static PlayerRPGStats defaultStats() {
    Map<PlayerRPGStatType, PlayerRPGStatValue> stats = new HashMap<>();
    for (var statType : PlayerRPGStatType.values()) {
      stats.put(statType, PlayerRPGStatValue.createDefault());
    }
    return new PlayerRPGStats(stats);
  }


  public double getNormalized(final PlayerRPGStatType playerRPGStatType) {
    return stats.get(playerRPGStatType).getValue() / 100.0;
  }

}
