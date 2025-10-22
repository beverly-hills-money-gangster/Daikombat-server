package com.beverly.hills.money.gang.util;

public interface MathUtil {

  static float normalizeMapCoordinate(
      final float coordinate,
      final float tileSize,
      final float mapSize) {
    return coordinate / tileSize - mapSize / 2f;
  }

  static float denormalizeMapCoordinate(
      final float coordinate,
      final float tileSize,
      final float mapSize) {
    return (coordinate + mapSize / 2f) * tileSize;
  }

  static int byteToInt(byte[] bytes, int offset) {
    return ((bytes[offset] & 0xFF)) |
        ((bytes[offset + 1] & 0xFF) << 8) | ((bytes[offset + 2] & 0xFF) << 16) | (
        (bytes[offset + 3] & 0xFF) << 24);
  }

}
