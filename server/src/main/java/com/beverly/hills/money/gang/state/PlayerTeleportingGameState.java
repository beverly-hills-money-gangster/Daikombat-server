package com.beverly.hills.money.gang.state;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerTeleportingGameState {

  private final PlayerStateReader teleportedPlayer;

}
