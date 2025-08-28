package com.beverly.hills.money.gang.util;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;

public interface ConcurrencyUtil {

  /**
   * Multiplies given integer by given coefficient. Mutates integer. Thread-safe.
   */
  static void multiplyAtomic(final @NonNull AtomicInteger integer, final double coefficient) {
    var current = integer.get();
    var expected = (int) (current * coefficient);
    if (integer.compareAndSet(current, expected)) {
      return;
    }
    multiplyAtomic(integer, coefficient);
  }
}
