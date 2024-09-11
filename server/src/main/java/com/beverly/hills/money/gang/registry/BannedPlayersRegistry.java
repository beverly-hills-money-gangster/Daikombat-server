package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.scheduler.Scheduler;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BannedPlayersRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(BannedPlayersRegistry.class);

  private final Set<String> bannedIps = ConcurrentHashMap.newKeySet();

  private final Scheduler scheduler;

  public void ban(String ipAddress) {
    LOG.info("Ban player {}", ipAddress);
    bannedIps.add(ipAddress);
    scheduler.schedule(ServerConfig.BAN_TIMEOUT_MLS, () -> {
      // nobody is banned by IP forever because of dynamic IP addressing
      LOG.info("Player {} is not banned anymore", ipAddress);
      bannedIps.remove(ipAddress);
    });
  }

  public boolean isBanned(String ipAddress) {
    return bannedIps.contains(ipAddress);
  }


}
