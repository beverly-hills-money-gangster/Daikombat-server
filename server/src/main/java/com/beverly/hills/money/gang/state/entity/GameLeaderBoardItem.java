package com.beverly.hills.money.gang.state.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Builder
@Getter
@ToString
public class GameLeaderBoardItem {

  private final int playerId;

  @NonNull
  private final String playerName;

  private final int kills;

  private final int deaths;
}
