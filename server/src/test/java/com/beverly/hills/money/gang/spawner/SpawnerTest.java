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
          .builder().teleports(7).spawns(12).powerUps(7).walls(76).build(),
      "crazy", ExpectedMapValues
          .builder().teleports(4).spawns(4).powerUps(6).walls(100).build(),
      "horror", ExpectedMapValues
          .builder().teleports(2).spawns(5).powerUps(6).walls(63).build(),
      "peace", ExpectedMapValues
          .builder().teleports(0).spawns(6).powerUps(0).walls(20).build()
  );

  @Test
  public void testMap() {
    for (Entry<String, ExpectedMapValues> expectedMapValues : EXPECTED.entrySet()) {
      String mapName = expectedMapValues.getKey();
      Spawner spawner = new Spawner(
          mapRegistry.getMap(expectedMapValues.getKey()).orElseThrow().getMapData());
      assertEquals(expectedMapValues.getValue().powerUps, spawner.getPowerUps().size(),
          "Power-ups mismatch in map " + mapName);
      assertEquals(expectedMapValues.getValue().teleports, spawner.getTeleports().size(),
          "Teleports mismatch in map " + mapName);
      assertEquals(expectedMapValues.getValue().spawns, spawner.getPlayerSpawns().size(),
          "Spawns mismatch in map " + mapName);
      assertEquals(expectedMapValues.getValue().walls, spawner.getAllWalls().size(),
          "Walls mismatch in map " + mapName);
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
  public void testNoFloorTiles() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("no_floor_tiles").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Map has no floor tiles"));
  }

  @Test
  public void testSpawnOutOfBounds() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("spawn_out_of_bounds").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("All spawns should be on floor tiles"));
  }

  @Test
  public void testSpawnInsideWall() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("spawn_in_the_wall").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("All spawns should be on floor tiles"));
  }

  @Test
  public void testTeleportOutOfBounds() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("teleport_out_of_bounds").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("All teleports should teleport to floor tiles"));
  }

  @Test
  public void testNoPlayerSpawn() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(invalidMapRegistry.getMap("no_player_spawn").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Map doesn't have player spawns"),
        "Should fail as no player spawn exists on the map. Actual exception message is: "
            + ex.getMessage());
  }

  @Test
  public void testTooBig() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(invalidMapRegistry.getMap("too_big").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Map is too big"));
  }

  @Test
  public void testWrongWallSize() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("walls_wrong_size").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Wall size should be divisible by 16"));
  }

  @Test
  public void testDecorationsNoName() {
    var ex = assertThrows(IllegalStateException.class,
        () -> new Spawner(
            invalidMapRegistry.getMap("decorations_no_name").orElseThrow().getMapData()));
    assertTrue(ex.getMessage().startsWith("Decoration missing name"));
  }


  @Builder
  @Getter
  private static class ExpectedMapValues {

    private final int teleports;
    private final int spawns;
    private final int powerUps;
    private final int walls;
  }

}
