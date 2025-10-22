package com.beverly.hills.money.gang.exception;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class GameErrorCodeTest {

  @Test
  public void testUniqueErrorCodes() {
    int errors = GameErrorCode.values().length;
    int uniqueErrorCodes = Arrays.stream(GameErrorCode.values())
        .map(GameErrorCode::getErrorCode).collect(Collectors.toSet()).size();
    if (errors != uniqueErrorCodes) {
      fail("All errors must have a unique error code");
    }
  }

}
