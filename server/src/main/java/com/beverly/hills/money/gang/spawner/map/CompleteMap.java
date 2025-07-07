package com.beverly.hills.money.gang.spawner.map;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class CompleteMap {

  @NonNull
  private final GameMapAssets assets;
  @NonNull
  private final MapData mapData;

}
