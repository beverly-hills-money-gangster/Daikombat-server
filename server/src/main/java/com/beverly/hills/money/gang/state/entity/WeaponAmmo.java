package com.beverly.hills.money.gang.state.entity;

import java.util.concurrent.atomic.AtomicInteger;

public class WeaponAmmo {

  private final AtomicInteger ammoCounter;

  public WeaponAmmo(final int maxAmmo) {
    ammoCounter = new AtomicInteger(maxAmmo);
  }

  public int getCurrentAmmo() {
    return ammoCounter.get();
  }

  public boolean wasteAmmo() {
    int currentValue = ammoCounter.get();
    if (currentValue == 0) {
      return false;
    }
    boolean success = ammoCounter.compareAndSet(currentValue, currentValue - 1);
    if (success) {
      return true;
    } else {
      return wasteAmmo();
    }
  }

}
