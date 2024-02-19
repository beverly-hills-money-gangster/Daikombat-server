package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

import java.util.List;


@Getter
@Builder
public class PlayerConnectedGameState {

    private final PlayerState playerState;

    @Builder.Default
    private final List<GameLeaderBoardItem> leaderBoard = List.of();

}
