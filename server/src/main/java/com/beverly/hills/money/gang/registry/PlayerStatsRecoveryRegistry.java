package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.scheduler.Scheduler;
import com.beverly.hills.money.gang.state.PlayerGameStatsReader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PlayerStatsRecoveryRegistry {

  private final Scheduler scheduler;

  private final Map<Integer, PlayerGameStatsReader> stats = new ConcurrentHashMap<>();

  public void saveStats(int playerId, PlayerGameStatsReader playerGameStats) {
    stats.put(playerId, playerGameStats);
    scheduler.schedule(ServerConfig.PLAYER_STATS_TIMEOUT_MLS, () -> stats.remove(playerId));
  }

  public Optional<PlayerGameStatsReader> getStats(int playerId) {
    return Optional.ofNullable(stats.get(playerId));
  }

  public void removeStats(int playerId) {
    stats.remove(playerId);
  }

}
