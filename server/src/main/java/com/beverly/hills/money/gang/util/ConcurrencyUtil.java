package com.beverly.hills.money.gang.util;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;

public interface ConcurrencyUtil {

  // TODO test it
  static void multiplyAtomic(
      final @NonNull AtomicInteger integer,
      double coefficient) {
    if (coefficient < 0) {
      throw new IllegalArgumentException("Coefficient can't be negative");
    }
    var current = integer.get();
    var expected = (int) (current * coefficient);
    if (integer.compareAndSet(current, expected)) {
      return;
    }
    multiplyAtomic(integer, coefficient);
  }
}
