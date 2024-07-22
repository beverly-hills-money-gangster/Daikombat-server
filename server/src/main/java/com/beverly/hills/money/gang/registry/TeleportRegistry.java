package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.state.PlayerState.PlayerCoordinates;
import com.beverly.hills.money.gang.state.Vector;
import com.beverly.hills.money.gang.teleport.Teleport;
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
    this(List.of(
        Teleport.builder().id(1)
            .location(Vector.builder().x(-25f).y(23f).build())
            .teleportCoordinates(PlayerCoordinates.builder()
                .position(Vector.builder().x(-4.8943987f).y(21.556356f).build())
                .direction(Vector.builder().x(0.0076999734f).y(-0.99996966f).build())
                .build())
            .build(),
        Teleport.builder().id(2)
            .location(Vector.builder().x(-4.95f).y(23.0f).build())
            .teleportCoordinates(
                PlayerCoordinates.builder()
                    .position(Vector.builder().x(-22.39956f).y(23.152378f).build())
                    .direction(Vector.builder().x(0.9999982f).y(-0.0021766382f).build())
                    .build()
            )
            .build(),
        Teleport.builder().id(3)
            .location(Vector.builder().x(-4.95f).y(13.0f).build())
            .teleportCoordinates(
                PlayerCoordinates.builder()
                    .position(Vector.builder().x(-22.39956f).y(23.152378f).build())
                    .direction(Vector.builder().x(0.9999982f).y(-0.0021766382f).build())
                    .build()
            )
            .build())
    );
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

  public Iterable<Teleport> getAllTeleports() {
    return teleportMap.values();
  }

  public Optional<Teleport> getTeleport(int teleportId) {
    return Optional.ofNullable(teleportMap.get(teleportId));
  }

}
