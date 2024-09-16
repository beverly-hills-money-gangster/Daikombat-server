package com.beverly.hills.money.gang.state.entity;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameOverGameState {

  @Builder.Default
  private List<GameLeaderBoardItem> leaderBoardItems = new ArrayList<>();

}
