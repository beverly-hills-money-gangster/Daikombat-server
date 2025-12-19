package com.beverly.hills.money.gang.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class PlayerGameId {

  private final int gameId;
  private final int playerId;
}
