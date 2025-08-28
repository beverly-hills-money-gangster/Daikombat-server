package com.beverly.hills.money.gang.state.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.registry.LocalMapRegistry;
import com.beverly.hills.money.gang.spawner.factory.ProdSpawnerFactory;
import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.GameWeaponType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class AmmoStorageTest {

  private AmmoStorage warriorAmmoStorage;

  private AmmoStorage skeletonAmmoStorage;

  @BeforeEach
  public void setUp() throws IOException {
    warriorAmmoStorage = new AmmoStorage(
        new Game(new LocalMapRegistry(),
            mock(), mock(), mock(),
            new ProdSpawnerFactory(), mock(),
            mock(), mock()), RPGPlayerClass.WARRIOR);

    skeletonAmmoStorage = new AmmoStorage(
        new Game(new LocalMapRegistry(),
            mock(), mock(), mock(),
            new ProdSpawnerFactory(), mock(),
            mock(), mock()), RPGPlayerClass.ANGRY_SKELETON);
  }


  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestoreZeroPercent(GameWeaponType gameWeaponType) {
    var ammoBefore = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    warriorAmmoStorage.restore(gameWeaponType, 0);
    var ammoAfter = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    assertEquals(ammoBefore, ammoAfter,
        "Ammo shouldn't change because we effectively didn't increase it");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestore50Percent(GameWeaponType gameWeaponType) {
    var ammoBefore = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(warriorAmmoStorage.wasteAmmo(gameWeaponType));
    }

    warriorAmmoStorage.restore(gameWeaponType, 0.5f);
    var ammoAfter = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);

    assertEquals(ammoBefore / 2, ammoAfter, "Only half of ammo should be restored");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestore100Percent(GameWeaponType gameWeaponType) {
    var ammoBefore = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(warriorAmmoStorage.wasteAmmo(gameWeaponType));
    }

    warriorAmmoStorage.restore(gameWeaponType, 1f);
    var ammoAfter = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);

    assertEquals(ammoBefore, ammoAfter, "All ammo should be restored");
  }


  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestore100PercentTwice(GameWeaponType gameWeaponType) {
    var ammoBefore = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(warriorAmmoStorage.wasteAmmo(gameWeaponType));
    }

    warriorAmmoStorage.restore(gameWeaponType, 1f);
    warriorAmmoStorage.restore(gameWeaponType, 1f); // restore again

    var ammoAfter = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);

    assertEquals(ammoBefore, ammoAfter, "All ammo should be restored");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testWasteAllAmmo(GameWeaponType gameWeaponType) {
    var ammoBefore = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(warriorAmmoStorage.wasteAmmo(gameWeaponType));
    }

    assertFalse(warriorAmmoStorage.wasteAmmo(gameWeaponType),
        "No ammo can be wasted because we have wasted it all before");

    var ammoAfter = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    assertEquals(0, ammoAfter);
  }


  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testWasteAmmoNotSupported(GameWeaponType gameWeaponType) {
    if (RPGPlayerClass.ANGRY_SKELETON.getWeapons().contains(gameWeaponType)) {
      // ignore supported weapons
      return;
    } else if (gameWeaponType == GameWeaponType.PUNCH) {
      // ignore melee weapons
      return;
    }
    assertFalse(skeletonAmmoStorage.wasteAmmo(gameWeaponType),
        "Should be no ammo because this weapon is not supported");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestoreAmmoNotSupported(GameWeaponType gameWeaponType) {
    if (RPGPlayerClass.ANGRY_SKELETON.getWeapons().contains(gameWeaponType)) {
      // ignore supported weapons
      return;
    } else if (gameWeaponType == GameWeaponType.PUNCH) {
      // ignore melee weapons
      return;
    }
    skeletonAmmoStorage.restore(gameWeaponType, 1);
    assertFalse(skeletonAmmoStorage.wasteAmmo(gameWeaponType),
        "Should be no ammo after restoring anyway because this weapon is not supported");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testWasteAllAmmoMultiThread(GameWeaponType gameWeaponType) {
    var ammoBefore = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    var executorService = Executors.newFixedThreadPool(16);
    try {
      var futures = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < ammoBefore; i++) {
        futures.add(executorService.submit(() -> warriorAmmoStorage.wasteAmmo(gameWeaponType)));
      }

      futures.forEach(future -> {
        try {
          var wasted = future.get(1, TimeUnit.SECONDS);
          assertTrue(wasted, "All threads should succeed wating ammo");
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });

      assertFalse(warriorAmmoStorage.wasteAmmo(gameWeaponType),
          "No ammo can be wasted because we have wasted it all before");

      var ammoAfter = warriorAmmoStorage.getCurrentAmmo(gameWeaponType);
      assertEquals(0, ammoAfter);
    } finally {
      executorService.shutdownNow();
    }
  }

}
