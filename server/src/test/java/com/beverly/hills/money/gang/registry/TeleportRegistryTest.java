package com.beverly.hills.money.gang.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.beverly.hills.money.gang.state.entity.Vector;
import com.beverly.hills.money.gang.state.entity.VectorDirection;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TeleportRegistryTest {

  @Test
  public void testNoTeleports() {
    var registry = new TeleportRegistry(List.of());
    assertTrue(registry.getAllTeleports().isEmpty());
  }

  @Test
  public void testSelfReference() {
    var ex = assertThrows(IllegalStateException.class, () -> new TeleportRegistry(
        List.of(Teleport.builder().id(1).teleportToId(1) // cyclic teleport
            .direction(VectorDirection.EAST)
            .spawnTo(Vector.builder().build())
            .location(Vector.builder().build())
            .build())));
    assertTrue(ex.getMessage().startsWith("Teleport 1 is referencing itself"));
  }

  @Test
  public void testReferenceNonExisting() {
    var ex = assertThrows(IllegalStateException.class, () -> new TeleportRegistry(
        List.of(Teleport.builder().id(1).teleportToId(666) // non-existing teleport
            .direction(VectorDirection.EAST)
            .spawnTo(Vector.builder().build())
            .location(Vector.builder().build())
            .build())));
    assertTrue(ex.getMessage().startsWith("Teleport 1 is referencing a non-existing teleport 666"));
  }

  @Test
  public void testSuccess() {
    var teleportsToCreate = List.of(Teleport.builder().id(1).teleportToId(2)
            .direction(VectorDirection.EAST)
            .spawnTo(Vector.builder().build())
            .location(Vector.builder().build())
            .build(),
        Teleport.builder().id(2).teleportToId(1)
            .direction(VectorDirection.EAST)
            .spawnTo(Vector.builder().build())
            .location(Vector.builder().build())
            .build());
    var registry = new TeleportRegistry(teleportsToCreate);
    assertEquals(teleportsToCreate.size(), registry.getAllTeleports().size());
    for (Teleport teleport : teleportsToCreate) {
      assertTrue(registry.getTeleport(teleport.getId()).isPresent());
    }
  }

}
