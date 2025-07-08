package com.beverly.hills.money.gang.spawner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.registry.LocalMapRegistry;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpawnerTest {

  private LocalMapRegistry mapRegistry;
  private LocalMapRegistry invalidMapRegistry;

  @BeforeEach
  public void setUp() throws IOException {
    mapRegistry = new LocalMapRegistry();
    invalidMapRegistry = new LocalMapRegistry("test_maps");
  }

  @AfterEach
  public void tearDown() {
    mapRegistry.close();
    invalidMapRegistry.close();
  }

  private final Map<String, ExpectedMapValues> EXPECTED = Map.of(
      "classic", ExpectedMapValues
          .builder().teleports(4).spawns(12).powerUps(6).build(),
      "crazy", ExpectedMapValues
          .builder().teleports(4).spawns(4).powerUps(6).build(),
      "horror", ExpectedMapValues
          .builder().teleports(2).spawns(5).powerUps(6).build(),
      "peace", ExpectedMapValues
          .builder().teleports(0).spawns(6).powerUps(0).build()
  );

  @Test
  public void testMap() {
    for (Entry<String, ExpectedMapValues> expectedMapValues : EXPECTED.entrySet()) {
      String mapName = expectedMapValues.getKey();
      Spawner spawner = new Spawner(
          mapRegistry.getMap(expectedMapValues.getKey()).orElseThrow().getMapData());
      assertEquals(spawner.getPowerUps().size(), expectedMapValues.getValue().powerUps,
          "Power-ups mismatch in map " + mapName);
      assertEquals(spawner.getTeleports().size(), expectedMapValues.getValue().teleports,
          "Teleports mismatch in map " + mapName);
      assertEquals(spawner.getPlayerSpawns().size(), expectedMapValues.getValue().spawns,
          "Spawns mismatch in map " + mapName);
    }
  }

  @Test
  public void testInvalidPowerUps() {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("invalid_power_ups").orElseThrow().getMapData()));
    assertTrue(ex.getMessage()
        .startsWith("No enum constant com.beverly.hills.money.gang.powerup.PowerUpType"));
  }

  @Test
  public void testNoPlayerSpawn() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(invalidMapRegistry.getMap("no_player_spawn").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Map doesn't have player spawns"));
  }

  @Test
  public void testTooBig() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(invalidMapRegistry.getMap("too_big").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Map is too big"));
  }

  @Builder
  @Getter
  private static class ExpectedMapValues {

    private final int teleports;
    private final int spawns;
    private final int powerUps;
  }

}
