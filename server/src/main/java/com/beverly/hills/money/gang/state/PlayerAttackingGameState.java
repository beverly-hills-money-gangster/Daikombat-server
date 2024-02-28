package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerAttackingGameState {

    private final PlayerStateReader attackingPlayer;

    private final PlayerStateReader playerAttacked;
}
