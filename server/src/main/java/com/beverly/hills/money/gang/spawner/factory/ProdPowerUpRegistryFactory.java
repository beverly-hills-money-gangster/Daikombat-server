package com.beverly.hills.money.gang.spawner.factory;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.registry.PowerUpRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProdPowerUpRegistryFactory extends AbstractPowerUpRegistryFactory {

  @Override
  public PowerUpRegistry create(List<PowerUp> powerUps) {
    return new PowerUpRegistry(powerUps);
  }
}
