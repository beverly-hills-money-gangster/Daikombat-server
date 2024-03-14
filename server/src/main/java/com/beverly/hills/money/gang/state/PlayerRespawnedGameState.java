package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerRespawnedGameState {
    private final PlayerState playerState;
}
