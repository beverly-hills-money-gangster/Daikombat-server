package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.PlayerStateReader;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerAttackingGameState {

  private final PlayerStateReader attackingPlayer;

  private final PlayerStateReader playerAttacked;

  @Nullable
  private final GameOverGameState gameOverState;

}
