package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlayerShootingGameState extends GameState {


    private final PlayerStateReader shootingPlayer;

    private final PlayerStateReader playerShot;


    @Builder
    public PlayerShootingGameState(long newGameStateId, PlayerStateReader shootingPlayer, PlayerStateReader playerShot) {
        super(newGameStateId);
        this.shootingPlayer = shootingPlayer;
        this.playerShot = playerShot;
    }
}
