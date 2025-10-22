package com.beverly.hills.money.gang.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MathUtilTest {

  @Test
  public void testBytesToIntZero() {
    byte[] bytes = {0, 0, 0, 0};
    assertEquals(0, MathUtil.byteToInt(bytes, 0));
  }

  @Test
  public void testBytesToInt31() {
    byte[] bytes = {0x1F, 0x00, 0x00, 0x00};
    assertEquals(31, MathUtil.byteToInt(bytes, 0));
  }


  @Test
  public void testBytesToInt31Offset4() {
    byte[] bytes = {0, 0, 0, 0, 0x1F, 0x00, 0x00, 0x00};
    assertEquals(31, MathUtil.byteToInt(bytes, 4));
  }


  @Test
  public void testDeNormalizeMapCoordinateReverse() {
    int tileSize = 16;
    int maxSize = 256;
    for (int i = 0; i < 2048; i++) {
      assertEquals(MathUtil.normalizeMapCoordinate(i, tileSize, maxSize),
          MathUtil.normalizeMapCoordinate(
              MathUtil.denormalizeMapCoordinate(
                  MathUtil.normalizeMapCoordinate(i, tileSize, maxSize),
                  tileSize, maxSize), tileSize, maxSize), 0.0001f);
    }
  }

}
