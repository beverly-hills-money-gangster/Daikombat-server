package com.beverly.hills.money.gang.spawner.factory;

import com.beverly.hills.money.gang.spawner.AbstractSpawner;
import com.beverly.hills.money.gang.spawner.map.MapData;

public abstract class AbstractSpawnerFactory {

  public abstract AbstractSpawner create(MapData mapData);
}
