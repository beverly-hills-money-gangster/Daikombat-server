package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlayerShootingGameState {

    private final PlayerStateReader shootingPlayer;

    private final PlayerStateReader playerShot;

    @Builder.Default
    private final List<GameLeaderBoardItem> leaderBoard = List.of();

}
