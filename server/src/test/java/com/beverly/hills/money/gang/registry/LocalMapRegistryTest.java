package com.beverly.hills.money.gang.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

public class LocalMapRegistryTest {

  @Test
  public void testNotExistingFolder() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new LocalMapRegistry("not_existing_folder"));
    assertTrue(ex.getMessage().startsWith("Map folder not found"));
  }

  @Test
  public void testLoadAllMaps() throws URISyntaxException {
    var registry = new LocalMapRegistry("maps");
    assertFalse(registry.getMapNames().isEmpty());
    for (String mapName : registry.getMapNames()) {
      assertTrue(registry.getMap(mapName).isPresent());
    }
  }

  @Test
  public void testLoadNonExistingMap() throws URISyntaxException {
    var registry = new LocalMapRegistry("maps");
    assertFalse(registry.getMap("non_existing_map").isPresent());
  }

  @Test
  public void testLoadIncompleteMaps() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new LocalMapRegistry("incomplete_maps"));
    assertTrue(ex.getMessage().startsWith("Can't load bytes"));
  }
}
