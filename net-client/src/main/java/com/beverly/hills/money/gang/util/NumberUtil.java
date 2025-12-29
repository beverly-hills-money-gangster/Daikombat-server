package com.beverly.hills.money.gang.util;

public interface NumberUtil {


  /**
   * Checks if given number is a valid probability
   *
   * @return true if valid. otherwise, false.
   */
  static boolean isValidProbability(float num) {
    return num >= 0 && num <= 1;
  }
}
