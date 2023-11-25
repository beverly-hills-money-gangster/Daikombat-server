package com.beverly.hills.money.gang.state;

public abstract class GameState {
    private final long newGameStateId;

    public GameState(long newGameStateId) {
        this.newGameStateId = newGameStateId;
    }

    public long getNewGameStateId() {
        return newGameStateId;
    }
}