package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;


public class PlayerConnectedGameState extends GameState {

    @Getter
    private final PlayerStateReader playerStateReader;

    @Builder
    public PlayerConnectedGameState(long newGameStateId, PlayerStateReader playerStateReader) {
        super(newGameStateId);
        this.playerStateReader = playerStateReader;
    }
}
