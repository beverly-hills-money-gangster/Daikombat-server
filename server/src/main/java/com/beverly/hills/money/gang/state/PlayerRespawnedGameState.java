package com.beverly.hills.money.gang.state;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerRespawnedGameState {

  private final PlayerState playerState;


  @Builder.Default
  private final List<GameLeaderBoardItem> leaderBoard = List.of();
}
