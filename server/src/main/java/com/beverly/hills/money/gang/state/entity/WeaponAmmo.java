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
    return ammoCounter.decrementAndGet() >= 0;
  }

}
