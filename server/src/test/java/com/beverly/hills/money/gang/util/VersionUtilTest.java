package com.beverly.hills.money.gang.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class VersionUtilTest {

  @Test
  public void testGetMajorVersion() {
    assertEquals(5, VersionUtil.getMajorVersion("5.0.1"));
  }

  @Test
  public void testGetMajorVersionSnapshot() {
    assertEquals(6, VersionUtil.getMajorVersion("6.33.1-SNAPSHOT"));
  }

  @Test
  public void testGetMajorVersionEmptyString() {
    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> VersionUtil.getMajorVersion(""));
    assertEquals("Version can't be blank", ex.getMessage());
  }

  @Test
  public void testGetMajorVersionWrongFormat() {
    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> VersionUtil.getMajorVersion("This is not even a version"));
    assertEquals("Invalid version format", ex.getMessage());
  }

  @Test
  public void testGetMajorVersionNoMajorDigit() {
    Exception ex = assertThrows(IllegalArgumentException.class,
        () -> VersionUtil.getMajorVersion("wrong.digit.format"));
    assertEquals("Can't get major version", ex.getMessage());
  }

}
