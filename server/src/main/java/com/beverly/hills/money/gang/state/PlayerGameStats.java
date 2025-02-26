package com.beverly.hills.money.gang.state;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.ToString;

@ToString
public class PlayerGameStats implements PlayerGameStatsReader {

  private final AtomicInteger kills = new AtomicInteger();
  private final AtomicInteger deaths = new AtomicInteger();


  public int getKills() {
    return kills.get();
  }

  public int getDeaths() {
    return deaths.get();
  }

  public void incKills() {
    kills.incrementAndGet();
  }

  public void incDeaths() {
    deaths.incrementAndGet();
  }

  public void setKills(int killsCount) {
    kills.set(killsCount);
  }

  public void setDeaths(int deathsCount) {
    deaths.set(deathsCount);
  }

}
