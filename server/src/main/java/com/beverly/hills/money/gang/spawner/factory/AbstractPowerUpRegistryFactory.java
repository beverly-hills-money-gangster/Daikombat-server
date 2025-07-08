package com.beverly.hills.money.gang.spawner.factory;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.registry.PowerUpRegistry;
import java.util.List;

public abstract class AbstractPowerUpRegistryFactory {

  public abstract PowerUpRegistry create(List<PowerUp> powerUps);
}
