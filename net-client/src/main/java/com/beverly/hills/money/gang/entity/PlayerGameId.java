package com.beverly.hills.money.gang.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerGameId {

  private final int gameId;
  private final int playerId;
}
