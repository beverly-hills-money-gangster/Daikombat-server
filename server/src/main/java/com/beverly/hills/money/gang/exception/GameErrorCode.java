package com.beverly.hills.money.gang.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum GameErrorCode {
  COMMON_ERROR(0),
  AUTH_ERROR(1),
  PLAYER_EXISTS(2),
  SERVER_FULL(3),
  NOT_EXISTING_GAME_ROOM(4),
  COMMAND_NOT_RECOGNIZED(5),
  CAN_NOT_ATTACK_YOURSELF(6),
  GAME_CLOSED(7),
  CHEATING(8),
  PLAYER_DOES_NOT_EXIST(9);

  @Getter
  private final int errorCode;

}
