package com.beverly.hills.money.gang.state;

import static com.beverly.hills.money.gang.exception.GameErrorCode.CHEATING;
import static com.beverly.hills.money.gang.exception.GameErrorCode.COMMON_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import io.netty.channel.Channel;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class GameTeleportTest extends GameTest {

  public GameTeleportTest() throws IOException {
  }

  /**
   * @given a player that stands too far from a teleport
   * @when the player tries to teleport
   * @then it fails
   */
  @Test
  public void testTeleportTooFar() throws GameLogicError {
    doReturn(true).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);
    var oldCoordinates = teleportedPlayerGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();
    var teleportToUse = teleportedPlayerGameState.getTeleports().get(0);

    GameLogicError error = assertThrows(GameLogicError.class, () -> game.teleport(
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        oldCoordinates,
        teleportToUse.getId(),
        testSequenceGenerator.getNext(),
        PING_MLS
    ));
    assertEquals(CHEATING, error.getErrorCode());
    assertEquals("Teleport is too far", error.getMessage());
    assertEquals(oldCoordinates,
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Coordinates should NOT change if there was an error");
  }

  /**
   * @given a player
   * @when the player tries to teleport using a non-existing teleport id
   * @then it fails
   */
  @Test
  public void testTeleportWrongTeleportId() throws GameLogicError {
    doReturn(false).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);
    var oldCoordinates = teleportedPlayerGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    GameLogicError error = assertThrows(GameLogicError.class, () -> game.teleport(
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getPlayerId(),
        oldCoordinates,
        666, // not real
        testSequenceGenerator.getNext(),
        PING_MLS
    ));
    assertEquals(COMMON_ERROR, error.getErrorCode());
    assertEquals("Can't find teleport", error.getMessage());
    assertEquals(oldCoordinates,
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Coordinates should NOT change if there was an error");
  }

  /**
   * @given a player
   * @when the player tries to teleport using a non-existing player id
   * @then it fails
   */
  @Test
  public void testTeleportWrongPlayerId() throws GameLogicError {
    doReturn(false).when(antiCheat).isTeleportTooFar(any(), any());
    String teleportedPlayerName = "teleported player";
    Channel channel = mock(Channel.class);

    PlayerJoinedGameState teleportedPlayerGameState = fullyJoin(teleportedPlayerName,
        channel, PlayerStateColor.GREEN);
    var oldCoordinates = teleportedPlayerGameState.getPlayerStateChannel().getPlayerState()
        .getCoordinates();

    GameLogicError error = assertThrows(GameLogicError.class, () -> game.teleport(
        666, // not real
        oldCoordinates,
        teleportedPlayerGameState.getTeleports().get(0).getId(),
        testSequenceGenerator.getNext(),
        PING_MLS
    ));
    assertEquals(COMMON_ERROR, error.getErrorCode());
    assertEquals("Can't find player", error.getMessage());
    assertEquals(oldCoordinates,
        teleportedPlayerGameState.getPlayerStateChannel().getPlayerState().getCoordinates(),
        "Coordinates should NOT change if there was an error");
  }
}
