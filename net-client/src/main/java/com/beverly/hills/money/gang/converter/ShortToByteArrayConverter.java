package com.beverly.hills.money.gang.converter;

public abstract class ShortToByteArrayConverter {

  public static byte[] toByteArray(short[] shorts) {
    byte[] byteArray = new byte[shorts.length * 2];

    for (int i = 0; i < shorts.length; i++) {
      int index = i * 2;

      // Store as little-endian (lower byte first, then higher byte)
      byteArray[index] = (byte) (shorts[i] & 0xFF);
      byteArray[index + 1] = (byte) ((shorts[i] >> 8) & 0xFF);
    }

    return byteArray;
  }

  public static short[] toShortArray(byte[] bytes) {
    if (bytes.length % 2 != 0) {
      throw new IllegalArgumentException("Byte array length must be even for 16-bit PCM.");
    }

    int shortCount = bytes.length / 2;
    short[] shortArray = new short[shortCount];

    for (int i = 0; i < shortCount; i++) {
      int index = i * 2;

      // Combine two bytes (little-endian) into a short
      shortArray[i] = (short) ((bytes[index] & 0xFF) | ((bytes[index + 1] & 0xFF) << 8));
    }

    return shortArray;
  }

}
