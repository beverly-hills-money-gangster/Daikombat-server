package com.beverly.hills.money.gang.registry;

import com.beverly.hills.money.gang.spawner.map.CompleteMap;
import java.util.Optional;

public interface MapRegistry {

  Optional<CompleteMap> getMap(String name);

}
