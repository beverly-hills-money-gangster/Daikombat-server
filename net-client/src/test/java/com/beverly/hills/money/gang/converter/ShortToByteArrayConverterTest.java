package com.beverly.hills.money.gang.converter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class ShortToByteArrayConverterTest {

  private final Random random = new Random();

  private final List<short[]> SHORTS = List.of(
      new short[]{1},
      new short[]{1, 2},
      new short[]{1, 2, 3},
      new short[]{-1, -2, -3},
      new short[4],
      new short[]{Short.MAX_VALUE},
      new short[]{Short.MIN_VALUE},
      new short[256]
  );

  private final List<byte[]> BYTES = List.of(
      new byte[]{1, 2},
      new byte[]{1, 2, 3, 4},
      new byte[4],
      new byte[]{Byte.MIN_VALUE, Byte.MAX_VALUE},
      new byte[]{},
      new byte[256]
  );

  @Test
  public void testToByteArray() {
    BYTES.forEach(bytes -> assertArrayEquals(bytes,
        ShortToByteArrayConverter.toByteArray(ShortToByteArrayConverter.toShortArray(bytes)),
        "Failed to convert bytes " + Arrays.toString(bytes)));
  }

  @RepeatedTest(256)
  public void testToByteArrayRandom() {
    byte[] bytes = new byte[512];
    random.nextBytes(bytes);
    assertArrayEquals(bytes,
        ShortToByteArrayConverter.toByteArray(ShortToByteArrayConverter.toShortArray(bytes)),
        "Failed to convert bytes " + Arrays.toString(bytes));
  }

  @Test
  public void testToShortArray() {
    SHORTS.forEach(shorts -> assertArrayEquals(shorts,
        ShortToByteArrayConverter.toShortArray(ShortToByteArrayConverter.toByteArray(shorts)),
        "Failed to convert shorts " + Arrays.toString(shorts)));
  }

}
