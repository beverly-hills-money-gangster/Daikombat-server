package com.beverly.hills.money.gang.spawner.map;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameMapAssets {

  private final byte[] atlasPng;

  private final byte[] atlasTsx;

  private final byte[] onlineMapTmx;

  private final String hash;

}
