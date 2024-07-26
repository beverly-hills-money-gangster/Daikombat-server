package com.beverly.hills.money.gang.registry;

import static com.beverly.hills.money.gang.spawner.Spawner.TELEPORTS;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TeleportRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(TeleportRegistry.class);

  private final Map<Integer, Teleport> teleportMap = new ConcurrentHashMap<>();

  public TeleportRegistry() {
    this(TELEPORTS);
  }

  private TeleportRegistry(List<Teleport> teleports) {
    if (!ServerConfig.TELEPORTS_ENABLED) {
      LOG.warn("Teleports not enabled!");
      return;
    }
    teleports.forEach(teleport -> {
      if (teleportMap.putIfAbsent(teleport.getId(), teleport) != null) {
        throw new IllegalStateException(
            "Multiple teleports have the same id. Check id " + teleport.getId());
      }
    });
  }

  public List<Teleport> getAllTeleports() {
    return new ArrayList<>(teleportMap.values());
  }

  public Optional<Teleport> getTeleport(int teleportId) {
    return Optional.ofNullable(teleportMap.get(teleportId));
  }

}
