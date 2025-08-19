package com.beverly.hills.money.gang.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class ConcurrencyUtilTest {

  @Test
  public void testMultiplyAtomicZero() {
    AtomicInteger integer = new AtomicInteger(0);
    ConcurrencyUtil.multiplyAtomic(integer, 0f);
    assertEquals(0, integer.get());

    integer = new AtomicInteger(1);
    ConcurrencyUtil.multiplyAtomic(integer, 0f);
    assertEquals(0, integer.get());

    integer = new AtomicInteger(0);
    ConcurrencyUtil.multiplyAtomic(integer, 1f);
    assertEquals(0, integer.get());
  }

  @Test
  public void testMultiplyAtomicOne() {
    int oldValue = 5;
    AtomicInteger integer = new AtomicInteger(oldValue);
    ConcurrencyUtil.multiplyAtomic(integer, 1f);
    assertEquals(oldValue, integer.get());
  }

  @Test
  public void testMultiplyBasic() {
    int oldValue = 5;
    float coefficient = 3;
    AtomicInteger integer = new AtomicInteger(oldValue);
    ConcurrencyUtil.multiplyAtomic(integer, coefficient);
    assertEquals(15, integer.get());
  }

  @Test
  public void testMultiplyDecimal() {
    int oldValue = 5;
    float coefficient = 3.5f;
    AtomicInteger integer = new AtomicInteger(oldValue);
    ConcurrencyUtil.multiplyAtomic(integer, coefficient);
    assertEquals(17, integer.get());
  }

  @RepeatedTest(256)
  public void testMultiplyConcurrency() throws InterruptedException {
    int threadsToCreate = 10;
    AtomicInteger integer = new AtomicInteger(1);
    var threads = new ArrayList<Thread>();
    var latch = new CountDownLatch(1);

    for (int i = 0; i < threadsToCreate; i++) {
      threads.add(new Thread(() -> {
        try {
          latch.await();
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        ConcurrencyUtil.multiplyAtomic(integer, 2);
      }));
    }
    threads.forEach(Thread::start);
    latch.countDown();

    for (Thread thread : threads) {
      thread.join();
    }

    assertEquals(1024, integer.get());

  }

}
