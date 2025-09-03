package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.exception.GameErrorCode.COMMON_ERROR;
import static com.beverly.hills.money.gang.exception.GameErrorCode.PLAYER_DOES_NOT_EXIST;
import static com.beverly.hills.money.gang.state.entity.PlayerState.DEFAULT_HP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.powerup.PowerUp;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerState.Coordinates;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import com.beverly.hills.money.gang.state.entity.Vector;
import io.netty.channel.Channel;
import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class GameTestRespawn extends GameTest {

  public GameTestRespawn() throws IOException {
  }

  /**
   * @given a game with 3 players: respawned, victim, killer. respawned kills victim, killer kills
   * respawned.
   * @when respawned respawns after getting killed
   * @then other players observe a respawn. respawned player stats(kills and deaths) are persisted
   */
  @Test
  public void testRespawnDead() throws GameLogicError {
    String respawnPlayerName = "some player";
    PlayerJoinedGameState playerRespawnedGameState = fullyJoin(respawnPlayerName,
        mock(Channel.class), PlayerStateColor.GREEN);
    PlayerJoinedGameState playerVictimGameState = fullyJoin("victim", mock(Channel.class),
        PlayerStateColor.GREEN);
    PlayerJoinedGameState killerPlayerConnectedGameState = fullyJoin("killer",
        mock(Channel.class), PlayerStateColor.GREEN);

    int shotsToKill = (int) Math.ceil(100d / game.getGameConfig().getDefaultShotgunDamage());

    // after this loop, victim player is dead
    for (int i = 0; i < shotsToKill; i++) {
      game.attack(
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          playerVictimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    // after this loop, respawn player is dead
    for (int i = 0; i < shotsToKill; i++) {
      game.bufferMove(
          playerVictimGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          playerVictimGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          testSequenceGenerator.getNext(), PING_MLS);
      game.attack(
          killerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
          killerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getCoordinates()
              .getPosition(),
          killerPlayerConnectedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
          GameWeaponType.SHOTGUN.getDamageFactory().getDamage(game),
          testSequenceGenerator.getNext(),
          PING_MLS);
    }

    var respawned = game.respawnPlayer(
        playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId());
    assertEquals(-1,
        respawned.getPlayerStateChannel().getPlayerState().getLastReceivedEventSequenceId(),
        "Last received sequence id must default to -1 because game client starts from 0 on respawn");
    assertEquals(playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        respawned.getPlayerStateChannel().getPlayerState().getPlayerId());
    assertFalse(respawned.getPlayerStateChannel().getPlayerState().isDead());
    assertEquals(1, respawned.getPlayerStateChannel().getPlayerState().getGameStats().getDeaths(),
        "Death count should increment after respawn");
    assertEquals(DEFAULT_HP,
        respawned.getPlayerStateChannel().getPlayerState().getHealth(),
        "Health must be restored after respawn");
    assertEquals(1, respawned.getPlayerStateChannel().getPlayerState().getGameStats().getKills(),
        "Number of kills should be the same after respawn");

    PlayerJoinedGameState observerPlayerConnectedGameState = fullyJoin("observer",
        mock(Channel.class), PlayerStateColor.GREEN);
    assertEquals(4, game.playersOnline(),
        "4 players must be online: respawned, victim, killer, and observer");
    assertEquals(4, observerPlayerConnectedGameState.getLeaderBoard().size(),
        "4 players must be in the board: respawned, victim, killer, and observer");

    var respawnedLeaderBoardItem = observerPlayerConnectedGameState.getLeaderBoard().stream()
        .filter(
            gameLeaderBoardItem -> gameLeaderBoardItem.getPlayerId()
                == respawned.getPlayerStateChannel()
                .getPlayerState().getPlayerId())
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("Can't find respawned player in the leaderboard"));
    assertEquals(1, respawnedLeaderBoardItem.getDeaths());
    assertEquals(1, respawnedLeaderBoardItem.getKills());

    assertEquals(5, respawned.getSpawnedPowerUps().size());
    assertEquals(
        Stream.of(quadDamagePowerUp, defencePowerUp, invisibilityPowerUp, healthPowerUp, beastPowerUp).sorted(
            Comparator.comparing(PowerUp::getType)).collect(Collectors.toList()),
        respawned.getSpawnedPowerUps().stream().sorted(
            Comparator.comparing(PowerUp::getType)).collect(Collectors.toList()));

    Coordinates newCoordinates = Coordinates
        .builder()
        .direction(Vector.builder().x(1f).y(0).build())
        .position(Vector.builder().x(0f).y(1).build()).build();

    game.bufferMove(
        playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        newCoordinates,
        0, PING_MLS);

    assertEquals(newCoordinates,
        playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Respawned player position should be updated after moving");
    assertEquals(0, playerRespawnedGameState.getPlayerStateChannel().getPlayerState()
            .getLastReceivedEventSequenceId(),
        "Last event sequence id must be 0 as it was the last id received after buffering a MOVE event");
  }

  /**
   * @given a game with 1 connected player
   * @when the player respawns alive
   * @then an error is thrown
   */
  @Test
  public void testRespawnAlive() throws GameLogicError {
    String respawnPlayerName = "some player";
    PlayerJoinedGameState playerRespawnedGameState = fullyJoin(respawnPlayerName,
        mock(Channel.class), PlayerStateColor.GREEN);
    GameLogicError gameLogicError
        = assertThrows(GameLogicError.class,
        () -> game.respawnPlayer(
            playerRespawnedGameState.getPlayerStateChannel().getPlayerState().getPlayerId()),
        "Live players shouldn't be able to respawn");
    assertEquals(COMMON_ERROR, gameLogicError.getErrorCode());
  }

  /**
   * @given a game with no players
   * @when a non-existing player respawns
   * @then an error is thrown
   */
  @Test
  public void testRespawnNonExisting() {
    GameLogicError gameLogicError
        = assertThrows(GameLogicError.class, () -> game.respawnPlayer(666),
        "Non-existing players shouldn't be able to respawn");
    assertEquals(PLAYER_DOES_NOT_EXIST, gameLogicError.getErrorCode());
  }
}
