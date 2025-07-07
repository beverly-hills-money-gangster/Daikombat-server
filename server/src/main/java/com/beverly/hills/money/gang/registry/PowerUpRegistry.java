package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.powerup.PowerUpType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerUpRegistry {

  private static final Logger LOG = LoggerFactory.getLogger(PowerUpRegistry.class);

  private final Map<PowerUpType, PowerUp> powerUpsMapping;

  public PowerUpRegistry(final List<PowerUp> powerUps) {
    powerUpsMapping = new ConcurrentHashMap<>();
    if (!ServerConfig.POWER_UPS_ENABLED) {
      LOG.warn("Power-ups not enabled!");
      return;
    }
    powerUps.forEach(powerUp -> {
      if (powerUpsMapping.put(powerUp.getType(), powerUp) != null) {
        throw new IllegalStateException("Can't have 2 power-ups with the same type");
      }
    });
  }

  public List<PowerUp> getAvailable() {
    return new ArrayList<>(powerUpsMapping.values());
  }

  public PowerUp get(PowerUpType powerUpType) {
    return powerUpsMapping.get(powerUpType);
  }

  public PowerUp take(PowerUpType powerUpType) {
    return powerUpsMapping.remove(powerUpType);
  }

  public boolean release(PowerUp powerUp) {
    return powerUpsMapping.putIfAbsent(powerUp.getType(), powerUp) == null;
  }
}
