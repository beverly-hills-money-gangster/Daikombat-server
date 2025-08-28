package com.beverly.hills.money.gang.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.beverly.hills.money.gang.exception.GameLogicError;
import com.beverly.hills.money.gang.state.entity.PlayerJoinedGameState;
import com.beverly.hills.money.gang.state.entity.PlayerStateColor;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

public class GameMergeTest extends GameTest {

  public GameMergeTest() throws IOException {
  }


  /**
   * @when a player tries to merge a connection using wrong(non-existing) player id
   * @then merging fails
   */
  @Test
  public void testMergeConnectionWrongPlayerId() {
    InetSocketAddress wrongIpAddress = mock(InetSocketAddress.class);
    InetAddress wrongInetAddress = mock(InetAddress.class);
    doReturn(wrongInetAddress).when(wrongIpAddress).getAddress();
    doReturn("8.8.8.8").when(wrongInetAddress).getHostAddress();
    Channel secondaryChannel = mock(Channel.class);
    doReturn(wrongIpAddress).when(secondaryChannel).remoteAddress();

    var ex = assertThrows(GameLogicError.class,
        () -> game.mergeConnection(666, secondaryChannel));
    assertEquals("Can't merge connections", ex.getMessage());
  }

  /**
   * @given a player with an established connection
   * @when a new connection tries to merge with the player's connection using a different IP
   * @then merging fails. IP should match.
   */
  @Test
  public void testMergeConnectionWrongIpAddress() throws GameLogicError {

    InetSocketAddress correctIpAddress = mock(InetSocketAddress.class);
    InetAddress correctInetAddress = mock(InetAddress.class);
    doReturn(correctInetAddress).when(correctIpAddress).getAddress();
    doReturn("127.0.0.1").when(correctInetAddress).getHostAddress();
    Channel primaryChannel = mock(Channel.class);
    doReturn(correctIpAddress).when(primaryChannel).remoteAddress();

    InetSocketAddress wrongIpAddress = mock(InetSocketAddress.class);
    InetAddress wrongInetAddress = mock(InetAddress.class);
    doReturn(wrongInetAddress).when(wrongIpAddress).getAddress();
    doReturn("8.8.8.8").when(wrongInetAddress).getHostAddress();
    Channel secondaryChannel = mock(Channel.class);
    doReturn(wrongIpAddress).when(secondaryChannel).remoteAddress();

    PlayerJoinedGameState player = fullyJoin("some player name",
        primaryChannel, PlayerStateColor.GREEN);

    var ex = assertThrows(GameLogicError.class,
        () -> game.mergeConnection(player.getPlayerStateChannel().getPlayerState().getPlayerId(),
            secondaryChannel),
        "We can't merge these connections because they have different IPs");
    assertEquals("Can't merge connections", ex.getMessage());
  }

  /**
   * @given a player with an established connection
   * @when a new connection tries to merge with the player's connection with the same IP
   * @then mering is successful
   */
  @Test
  public void testMergeConnection() throws GameLogicError {
    EventLoop eventLoop = mock(EventLoop.class);

    // execute whatever is scheduled
    doAnswer(invocationOnMock -> {
      var runnable = (Runnable) invocationOnMock.getArgument(0);
      runnable.run();
      return null;
    }).when(eventLoop).schedule(any(Runnable.class), anyLong(), any());

    InetSocketAddress correctIpAddress = mock(InetSocketAddress.class);
    InetAddress correctInetAddress = mock(InetAddress.class);
    doReturn(correctInetAddress).when(correctIpAddress).getAddress();
    doReturn("127.0.0.1").when(correctInetAddress).getHostAddress();
    Channel primaryChannel = mock(Channel.class);
    doReturn(eventLoop).when(primaryChannel).eventLoop();
    doReturn(correctIpAddress).when(primaryChannel).remoteAddress();

    Channel secondaryChannel = mock(Channel.class);
    doReturn(eventLoop).when(secondaryChannel).eventLoop();
    doReturn(correctIpAddress).when(secondaryChannel).remoteAddress();

    PlayerJoinedGameState player = fullyJoin("some player name",
        primaryChannel, PlayerStateColor.GREEN);

    game.mergeConnection(player.getPlayerStateChannel().getPlayerState().getPlayerId(),
        secondaryChannel);

    assertTrue(game.getPlayersRegistry().findPlayer(
            player.getPlayerStateChannel().getPlayerState().getPlayerId()).get()
        .isOurChannel(secondaryChannel), "Secondary connection should be 'ours'");
  }

}
