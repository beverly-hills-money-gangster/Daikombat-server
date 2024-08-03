package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.PlayerStateReader;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerTeleportingGameState {

  private final PlayerStateReader teleportedPlayer;

}
