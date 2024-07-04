package com.beverly.hills.money.gang.state;

import com.beverly.hills.money.gang.powerup.PowerUp;
import java.util.List;
import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class PlayerJoinedGameState {

  private final PlayerStateChannel playerStateChannel;

  private final List<PowerUp> spawnedPowerUps;

  @Builder.Default
  private final List<GameLeaderBoardItem> leaderBoard = List.of();

}
