package com.beverly.hills.money.gang.state.entity;

import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.state.PlayerStateChannel;
import com.beverly.hills.money.gang.teleport.Teleport;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;


@Getter
@Builder
public class PlayerJoinedGameState {

  @NonNull
  private final PlayerStateChannel playerStateChannel;

  @NonNull
  private final List<PowerUp> spawnedPowerUps;

  @NonNull
  private final List<Teleport> teleports;

  @Builder.Default
  private final List<GameLeaderBoardItem> leaderBoard = List.of();

}
