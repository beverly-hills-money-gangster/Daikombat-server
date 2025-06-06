package com.beverly.hills.money.gang.state.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.state.Game;
import com.beverly.hills.money.gang.state.GameWeaponType;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class AmmoStorageTest {

  private AmmoStorage ammoStorage;

  @BeforeEach
  public void setUp() {
    ammoStorage = new AmmoStorage(new Game(mock(), mock(), mock(), mock(), mock(), mock(), mock()));
  }


  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestoreZeroPercent(GameWeaponType gameWeaponType) {
    var ammoBefore = ammoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    ammoStorage.restore(gameWeaponType, 0);
    var ammoAfter = ammoStorage.getCurrentAmmo(gameWeaponType);
    assertEquals(ammoBefore, ammoAfter,
        "Ammo shouldn't change because we effectively didn't increase it");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestore50Percent(GameWeaponType gameWeaponType) {
    var ammoBefore = ammoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(ammoStorage.wasteAmmo(gameWeaponType));
    }

    ammoStorage.restore(gameWeaponType, 0.5f);
    var ammoAfter = ammoStorage.getCurrentAmmo(gameWeaponType);

    assertEquals(ammoBefore / 2, ammoAfter, "Only half of ammo should be restored");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestore100Percent(GameWeaponType gameWeaponType) {
    var ammoBefore = ammoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(ammoStorage.wasteAmmo(gameWeaponType));
    }

    ammoStorage.restore(gameWeaponType, 1f);
    var ammoAfter = ammoStorage.getCurrentAmmo(gameWeaponType);

    assertEquals(ammoBefore, ammoAfter, "All ammo should be restored");
  }


  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testRestore100PercentTwice(GameWeaponType gameWeaponType) {
    var ammoBefore = ammoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(ammoStorage.wasteAmmo(gameWeaponType));
    }

    ammoStorage.restore(gameWeaponType, 1f);
    ammoStorage.restore(gameWeaponType, 1f); // restore again

    var ammoAfter = ammoStorage.getCurrentAmmo(gameWeaponType);

    assertEquals(ammoBefore, ammoAfter, "All ammo should be restored");
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testWasteAllAmmo(GameWeaponType gameWeaponType) {
    var ammoBefore = ammoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    for (int i = 0; i < ammoBefore; i++) {
      assertTrue(ammoStorage.wasteAmmo(gameWeaponType));
    }

    assertFalse(ammoStorage.wasteAmmo(gameWeaponType),
        "No ammo can be wasted because we have wasted it all before");

    var ammoAfter = ammoStorage.getCurrentAmmo(gameWeaponType);
    assertEquals(0, ammoAfter);
  }

  @ParameterizedTest
  @EnumSource(GameWeaponType.class)
  public void testWasteAllAmmoMultiThread(GameWeaponType gameWeaponType) {
    var ammoBefore = ammoStorage.getCurrentAmmo(gameWeaponType);
    if (ammoBefore == null) {
      // skip
      return;
    }
    var executorService = Executors.newFixedThreadPool(16);
    try {
      var futures = new ArrayList<Future<Boolean>>();
      for (int i = 0; i < ammoBefore; i++) {
        futures.add(executorService.submit(() -> ammoStorage.wasteAmmo(gameWeaponType)));
      }

      futures.forEach(future -> {
        try {
          var wasted = future.get(1, TimeUnit.SECONDS);
          assertTrue(wasted, "All threads should succeed wating ammo");
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });

      assertFalse(ammoStorage.wasteAmmo(gameWeaponType),
          "No ammo can be wasted because we have wasted it all before");

      var ammoAfter = ammoStorage.getCurrentAmmo(gameWeaponType);
      assertEquals(0, ammoAfter);
    } finally {
      executorService.shutdownNow();
    }
  }

}
