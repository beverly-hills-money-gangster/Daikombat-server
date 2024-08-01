package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.PlayerStateReader;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerAttackingGameState {

  private final PlayerStateReader attackingPlayer;

  private final PlayerStateReader playerAttacked;

  @Builder.Default
  private final boolean gameOver = false;

}
