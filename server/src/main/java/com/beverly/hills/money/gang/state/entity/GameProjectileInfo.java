package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.state.GameProjectileType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameProjectileInfo {

  private final GameProjectileType gameProjectileType;

  private final double maxDistance;


}
