package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlayerShootingGameState extends GameState {

    private final int shootingPlayerId;

    private final PlayerShot playerShot;

    @Builder
    public PlayerShootingGameState(long newGameStateId, int shootingPlayerId, PlayerShot playerShot) {
        super(newGameStateId);
        this.shootingPlayerId = shootingPlayerId;
        this.playerShot = playerShot;
    }


    @Builder
    @Getter
    public static class PlayerShot {
        private final int shotPlayerId;
        private final int health;
        private final boolean dead;
    }

}
