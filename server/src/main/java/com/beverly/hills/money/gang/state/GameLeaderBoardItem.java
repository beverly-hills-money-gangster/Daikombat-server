package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class GameLeaderBoardItem {

    private final int playerId;

    private final String name;

    private final int kills;
}
