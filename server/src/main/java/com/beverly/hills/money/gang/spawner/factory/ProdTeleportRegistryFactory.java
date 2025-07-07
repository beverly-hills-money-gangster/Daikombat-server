package com.beverly.hills.money.gang.spawner.factory;

import com.beverly.hills.money.gang.registry.TeleportRegistry;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProdTeleportRegistryFactory extends AbstractTeleportRegistryFactory {

  @Override
  public TeleportRegistry create(List<Teleport> teleportList) {
    return new TeleportRegistry(teleportList);
  }
}
