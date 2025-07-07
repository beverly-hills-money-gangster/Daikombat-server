package com.beverly.hills.money.gang.spawner.factory;

import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;

public abstract class AbstractTeleportRegistryFactory {

  public abstract TeleportRegistry create(List<Teleport> teleportList);
}
