package com.beverly.hills.money.gang.exception;

public class GameLogicError extends Exception {

  private final GameErrorCode errorCode;

  public GameLogicError(String message, GameErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public GameErrorCode getErrorCode() {
    return errorCode;
  }
}
