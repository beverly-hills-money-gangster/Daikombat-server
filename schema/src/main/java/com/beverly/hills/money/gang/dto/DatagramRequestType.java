package com.beverly.hills.money.gang.dto;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum DatagramRequestType {
  KEEP_ALIVE((byte) 1),
  VOICE_CHAT((byte) 2),
  GAME_EVENT((byte) 3),
  ACK((byte) 4);

  @Getter
  private final byte code;

  public static DatagramRequestType create(final byte code) {
    return Arrays.stream(DatagramRequestType.values()).filter(
            datagramRequestType -> datagramRequestType.code == code).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Not supported code " + code));
  }
}
