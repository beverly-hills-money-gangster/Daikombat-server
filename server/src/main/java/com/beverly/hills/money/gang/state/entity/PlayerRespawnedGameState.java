package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.state.PlayerNetworkLayerState;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Getter
@Builder
public class PlayerRespawnedGameState {

  @NonNull
  private final PlayerNetworkLayerState playerNetworkLayerState;

  @NonNull
  private final List<PowerUp> spawnedPowerUps;

  @NonNull
  private final List<Teleport> teleports;

  @Builder.Default
  private final List<GameLeaderBoardItem> leaderBoard = List.of();
}
