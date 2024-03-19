package com.beverly.hills.money.gang.generator;

import java.util.concurrent.atomic.AtomicInteger;

public class IdGenerator {

  private final AtomicInteger generator = new AtomicInteger();

  public int getNext() {
    return generator.getAndIncrement();
  }
}
