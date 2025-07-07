package com.beverly.hills.money.gang.spawner.factory;

import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.spawner.Spawner;
import com.beverly.hills.money.gang.spawner.map.MapData;
import org.springframework.stereotype.Component;

@Component
public class ProdSpawnerFactory extends AbstractSpawnerFactory {

  @Override
  public AbstractSpawner create(MapData mapData) {
    return new Spawner(mapData);
  }
}
