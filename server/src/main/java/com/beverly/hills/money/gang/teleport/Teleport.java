package com.beverly.hills.money.gang.teleport;

import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.Vector;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Teleport {

  private final int id;

  private final Vector location;

  private final Coordinates teleportCoordinates;

}
