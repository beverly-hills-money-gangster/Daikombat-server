package com.beverly.hills.money.gang.teleport;

import com.beverly.hills.money.gang.state.PlayerState.PlayerCoordinates;
import com.beverly.hills.money.gang.state.Vector;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Teleport {

  private final int id;

  private final Vector location;

  private final PlayerCoordinates teleportCoordinates;

}
