package com.beverly.hills.money.gang.spawner.map;

import java.util.Optional;

public interface MapRegistry {

  Optional<CompleteMap> getMap(String name);

}
