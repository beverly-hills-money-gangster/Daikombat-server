package com.beverly.hills.money.gang.state;

import lombok.Getter;

public class PlayerRPGStatValue {

  private static final int MAX = 150;
  public static final int DEFAULT = 100;
  private static final int MIN = 50;

  @Getter
  private final int value;

  public PlayerRPGStatValue(int value) {
    if (value > MAX) {
      throw new IllegalStateException("Can't be greater than " + MAX);
    } else if (value < MIN) {
      throw new IllegalStateException("Can't be lower than " + MIN);
    }
    this.value = value;
  }

  public static PlayerRPGStatValue createDefault() {
    return new PlayerRPGStatValue(DEFAULT);
  }
}
