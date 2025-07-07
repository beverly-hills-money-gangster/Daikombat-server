package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeleportRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(TeleportRegistry.class);

  private final Map<Integer, Teleport> teleportMap = new ConcurrentHashMap<>();

  public TeleportRegistry(List<Teleport> teleports) {
    if (!ServerConfig.TELEPORTS_ENABLED) {
      LOG.warn("Teleports not enabled!");
      return;
    } else if (teleports.isEmpty()) {
      LOG.warn("No teleports created");
      return;
    }
    teleports.forEach(teleport -> {
      if (teleportMap.putIfAbsent(teleport.getId(), teleport) != null) {
        throw new IllegalStateException(
            "Multiple teleports have the same id. Check id " + teleport.getId());
      }
    });
    validateTeleports();
  }

  private void validateTeleports() {
    teleportMap.forEach((id, teleport) -> {
      if (teleport.getId().equals(teleport.getTeleportToId())) {
        throw new IllegalStateException(
            "Teleport " + id + " is referencing itself");
      } else if (!teleportMap.containsKey(teleport.getTeleportToId())) {
        throw new IllegalStateException(
            "Teleport " + id + " is referencing a non-existing teleport "
                + teleport.getTeleportToId());
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
