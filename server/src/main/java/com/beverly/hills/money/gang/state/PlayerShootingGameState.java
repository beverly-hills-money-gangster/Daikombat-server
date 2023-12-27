package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerShootingGameState {


    private final PlayerStateReader shootingPlayer;

    private final PlayerStateReader playerShot;

}
