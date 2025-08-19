package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.beverly.hills.money.gang.config.ServerConfig;
import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import io.netty.channel.Channel;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class GameCloseTest extends GameTest {

  public GameCloseTest() throws IOException {
  }

  /**
   * @given a game with no players
   * @when the game gets closed
   * @then nothing happens
   */
  @Test
  public void testCloseNobodyConnected() {
    game.close();
  }


  /**
   * @given a game with many players
   * @when the game gets closed
   * @then all players' channels get closed and no player is connected anymore
   */
  @Test
  public void testCloseSomebodyConnected() throws Throwable {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }
    game.close();
    // all channels should be closed
    verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
    assertEquals(0, game.playersOnline(), "No players online when game is closed");
    assertEquals(0, game.getPlayersRegistry().allPlayers().size(),
        "No players in the registry when game is closed");
  }

  /**
   * @given a closed game with many players
   * @when the game gets closed again
   * @then nothing happens. the game is still closed.
   */
  @Test
  public void testCloseTwice() throws GameLogicError {
    String playerName = "some player";
    Channel channel = mock(Channel.class);
    for (int i = 0; i < ServerConfig.MAX_PLAYERS_PER_GAME; i++) {
      fullyJoin(playerName + " " + i, channel, PlayerStateColor.GREEN);
    }
    game.close(); // close once
    game.close(); // close second time
    // all channels should be closed
    verify(channel, times(ServerConfig.MAX_PLAYERS_PER_GAME)).close();
    assertEquals(0, game.playersOnline(), "No players online when game is closed");
    assertEquals(0, game.getPlayersRegistry().allPlayers().size(),
        "No players in the registry when game is closed");
  }

}
