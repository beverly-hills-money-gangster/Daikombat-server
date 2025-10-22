package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.exception.GameLogicError;
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
   * @given a game with no live players (all dead)
   * @when getPlayerWithinDamageRadius() is called
   * @then nothing is returned
   */
  @Test
  public void testGetPlayerWithinDamageRadiusAllPlayersDead() throws GameLogicError {
    String shooterPlayerName = "shooter player";
    Channel channel = mock(Channel.class);
    var shooterPlayerConnectedGameState = fullyJoin(shooterPlayerName,
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

    assertTrue(game.getPlayerWithinDamageRadius(
            shooterPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
            Vector.builder().build(), 10).isEmpty(),
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
    var joined = fullyJoin(shooterPlayerName, channel, PlayerStateColor.GREEN);
    assertTrue(game.getPlayerWithinDamageRadius(
            joined.getPlayerStateChannel().getPlayerState().getPlayerId(),
            Vector.builder().build(), 0).isEmpty(),
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
        = game.getPlayerWithinDamageRadius(
        joinedPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        Vector.builder().build(), Integer.MAX_VALUE);

    assertFalse(playerWithinDamageRadius.isEmpty(),
        "Should not be empty because radius covers all players");

    assertEquals(
        joinedPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerWithinDamageRadius.get().getPlayerId());
  }

  /**
   * @given 2 players
   * @when getPlayerWithinDamageRadius() is called between the players
   * @then an enemy player is preferred
   */
  @Test
  public void testGetPlayerWithinDamageRadiusPreferEnemyPlayer() throws GameLogicError {
    String shooterPlayerName = "shooter player";
    String victimPlayerName = "victim player";

    doReturn(Coordinates.builder()
            .direction(Vector.builder().x(0).y(0).build())
            .position(Vector.builder().build()).build(),
        Coordinates.builder()
            .direction(Vector.builder().build())
            .position(Vector.builder().x(2.5f).y(0).build()).build())
        .when(spawner).getPlayerSpawn(any());

    Channel channel = mock(Channel.class);
    var joinedShooterPlayer = fullyJoin(shooterPlayerName, channel, PlayerStateColor.GREEN);
    var joinedVictimPlayer = fullyJoin(victimPlayerName, channel, PlayerStateColor.GREEN);

    // blow up between 2 players
    var playerWithinDamageRadius
        = game.getPlayerWithinDamageRadius(
        joinedShooterPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        Vector.builder().x(1).y(0).build(), 2);

    assertFalse(playerWithinDamageRadius.isEmpty(),
        "Should not be empty because radius covers all players");

    // prefer an enemy
    assertEquals(
        joinedVictimPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerWithinDamageRadius.get().getPlayerId(), "An enemy player should be preferred");
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

    var withinRangePlayer = fullyJoin("within range", channel, PlayerStateColor.GREEN);

    // far away and not even within radius
    doReturn(Coordinates.builder()
        .direction(Vector.builder().build())
        .position(Vector.builder().y(100).x(-100).build())
        .build())
        .when(spawner).getPlayerSpawn(any());
    fullyJoin("faraway player 1", channel, PlayerStateColor.GREEN);
    var joinedFarawayPlayer2 = fullyJoin("faraway player 2", channel, PlayerStateColor.GREEN);

    var playerWithinDamageRadius
        = game.getPlayerWithinDamageRadius(
        joinedFarawayPlayer2.getPlayerStateChannel().getPlayerState().getPlayerId(),
        Vector.builder().build(), 10);

    assertFalse(playerWithinDamageRadius.isEmpty(),
        "Should not be empty because radius covers at least one player");

    assertEquals(
        nearestPlayer.getPlayerStateChannel().getPlayerState().getPlayerId(),
        playerWithinDamageRadius.get().getPlayerId());
  }
}
