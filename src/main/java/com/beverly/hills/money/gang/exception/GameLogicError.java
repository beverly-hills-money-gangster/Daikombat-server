package com.beverly.hills.money.gang.exception;

public class GameLogicError extends Exception {

    // TODO create a factory for all game errors
    private final GameErrorCode errorCode;

    public GameLogicError(String message, GameErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public GameErrorCode getErrorCode() {
        return errorCode;
    }
}
