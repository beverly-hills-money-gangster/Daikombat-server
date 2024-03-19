package com.beverly.hills.money.gang.state;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class GameOverState {

  private List<GameLeaderBoardItem> leaderBoard;
}
