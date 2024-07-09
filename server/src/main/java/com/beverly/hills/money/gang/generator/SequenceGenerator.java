package com.beverly.hills.money.gang.generator;

import java.util.concurrent.atomic.AtomicInteger;

public class SequenceGenerator {

  private final AtomicInteger generator;

  public SequenceGenerator(int startValue) {
    generator = new AtomicInteger(startValue);
  }

  public SequenceGenerator() {
    this(-1);
  }


  public int getNext() {
    return generator.incrementAndGet();
  }
}
