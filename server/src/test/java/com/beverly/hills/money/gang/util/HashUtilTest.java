package com.beverly.hills.money.gang.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class HashUtilTest {

  @Test
  public void testHashSame() {
    byte[] bytes = {1, 2, 3, 4, 5, 6};
    assertEquals(HashUtil.hash(List.of(bytes)), HashUtil.hash(List.of(bytes)));
  }

}
