package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class GameMiscTest extends GameTest {

  public GameMiscTest() throws IOException {
  }


  /**
   * @given a game with no players
   * @when getPlayerWithinDamageRadius() is called
   * @then nothing is returned
   */
  @Test
  public void testGetPlayerWithinDamageRadiusNoPlayers() {
    assertTrue(game.getPlayerWithinDamageRadius(Vector.builder().build(), 10).isEmpty(),
        "Expected to be empty because we have no players at all");
  }

  /**
   * @given a game with no live players (all dead)
   * @when getPlayerWithinDamageRadius() is called
   * @then nothing is returned
   */
  @Test
  public void testGetPlayerWithinDamageRadiusAllPlayersDead() throws GameLogicError {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    PlayerJoinedGameState shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
        channel, PlayerStateColor.GREEN);

    // two shots should be enough to commit suicide
    for (int i = 0; i <= 2; i++) {
      game.attack(
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    assertTrue(game.getPlayerWithinDamageRadius(Vector.builder().build(), 10).isEmpty(),
        "Expected to be empty because we have no live players at all");
  }

  /**
   * @given a game with one live player
   * @when getPlayerWithinDamageRadius() is called with radius 0
   * @then nothing is returned
   */
  @Test
  public void testGetPlayerWithinDamageRadiusTooShort() throws GameLogicError {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    fullyJoin(shooterPlayerName, channel, PlayerStateColor.GREEN);
    assertTrue(game.getPlayerWithinDamageRadius(Vector.builder().build(), 0).isEmpty(),
        "Expected to be empty because damage radius is 0");
  }

  /**
   * @given a game with one live player
   * @when getPlayerWithinDamageRadius() is called with radius Integer.MAX_VALUE
   * @then the player is returned
   */
  @Test
  public void testGetPlayerWithinDamageRadiusOnePlayer() throws GameLogicError {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    var joinedPlayer = fullyJoin(shooterPlayerName, channel, PlayerStateColor.GREEN);

    var playerWithinDamageRadius
        = game.getPlayerWithinDamageRadius(Vector.builder().build(), Integer.MAX_VALUE);

    assertFalse(playerWithinDamageRadius.isEmpty(),
        "Should not be empty because radius covers all players");

    assertEquals(
        joinedPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerWithinDamageRadius.get().getPlayerId());
  }

  /**
   * @given a game with several live player
   * @when getPlayerWithinDamageRadius() is called with radius 10
   * @then the nearest player is returned
   */
  @Test
  public void testGetPlayerWithinDamageRadiusThreePlayers() throws GameLogicError {

    Channel channel = mock(Channel.class);

    // same position
    doReturn(Coordinates.builder()
        .direction(Vector.builder().build()).position(Vector.builder().build()).build())
        .when(spawner).getPlayerSpawn(any());

    var nearestPlayer = fullyJoin("nearest", channel, PlayerStateColor.GREEN);

    // almost the same position
    doReturn(Coordinates.builder()
        .direction(Vector.builder().build()).position(Vector.builder().x(1).y(1).build())
        .build())
        .when(spawner).getPlayerSpawn(any());

    var withinRangePlayer = fullyJoin("withing range", channel, PlayerStateColor.GREEN);

    // far away and not even within radius
    doReturn(Coordinates.builder()
        .direction(Vector.builder().build())
        .position(Vector.builder().y(100).x(-100).build())
        .build())
        .when(spawner).getPlayerSpawn(any());
    fullyJoin("faraway player 1", channel, PlayerStateColor.GREEN);
    fullyJoin("faraway player 2", channel, PlayerStateColor.GREEN);

    var playerWithinDamageRadius
        = game.getPlayerWithinDamageRadius(Vector.builder().build(), 10);

    assertFalse(playerWithinDamageRadius.isEmpty(),
        "Should not be empty because radius covers at least one player");

    assertEquals(
        nearestPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerWithinDamageRadius.get().getPlayerId());
  }
}
