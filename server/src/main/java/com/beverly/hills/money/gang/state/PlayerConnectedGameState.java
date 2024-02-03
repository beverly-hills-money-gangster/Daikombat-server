package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class PlayerConnectedGameState {

    @Getter
    private final PlayerState playerState;

}
